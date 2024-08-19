package ly.count.android.sdk;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import androidx.annotation.NonNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ModuleContent extends ModuleBase {

    private String currentContentChecksum = null;
    private final ImmediateRequestGenerator iRGenerator;
    Content contentInterface;
    CountlyTimer countlyTimer;
    private boolean shouldFetchContents = false;
    private final int contentUpdateInterval;
    private boolean shouldAddParamsToRequest = false;
    private final boolean experimental = true;
    private String[] categories = null;
    private Intent intent = null;
    private final ContentCallback globalContentCallback;

    ModuleContent(@NonNull Countly cly, @NonNull CountlyConfig config) {
        super(cly, config);
        L.v("[ModuleContent] Initialising");
        iRGenerator = config.immediateRequestGenerator;

        L.d("[ModuleContent] Setting if remote config Automatic triggers enabled, " + config.enableRemoteConfigAutomaticDownloadTriggers + ", caching enabled: " + config.enableRemoteConfigValueCaching + ", auto enroll enabled: " + config.enableAutoEnrollFlag);
        contentInterface = new Content();
        countlyTimer = new CountlyTimer();
        contentUpdateInterval = config.contents.contentUpdateInterval;
        globalContentCallback = config.contents.globalContentCallback;
    }

    void fetchContentsInternal(String[] categories) {
        L.d("[ModuleContent] fetchContentsInternal, shouldFetchContents:[" + shouldFetchContents + "], categories:[" + Arrays.toString(categories) + "]");

        DisplayMetrics displayMetrics = deviceInfo.mp.getDisplayMetrics(_cly.context_);
        String requestData = prepareContentFetchRequest(displayMetrics);

        ConnectionProcessor cp = requestQueueProvider.createConnectionProcessor();
        final boolean networkingIsEnabled = cp.configProvider_.getNetworkingEnabled();

        iRGenerator.CreateImmediateRequestMaker().doWork(requestData, "/o/sdk/content", cp, false, networkingIsEnabled, checkResponse -> {
            L.d("[ModuleContent] fetchContentsInternal, processing fetched contents, received response is :[" + checkResponse + "]");
            if (checkResponse == null) {
                return;
            }

            try {
                if (validateResponse(checkResponse)) {
                    L.d("[ModuleContent] fetchContentsInternal, got new content data, showing it");
                    Map<Integer, TransparentActivityConfig> placementCoordinates = parseContent(checkResponse, displayMetrics);
                    if (placementCoordinates.isEmpty()) {
                        L.d("[ModuleContent] fetchContentsInternal, placement coordinates are empty, skipping");
                        return;
                    } else if (intent != null) {
                        _cly.context_.stopService(intent);
                    }

                    intent = new Intent(_cly.context_, TransparentActivity.class);
                    intent.putExtra(TransparentActivity.CONFIGURATION_LANDSCAPE, placementCoordinates.get(Configuration.ORIENTATION_LANDSCAPE));
                    intent.putExtra(TransparentActivity.CONFIGURATION_PORTRAIT, placementCoordinates.get(Configuration.ORIENTATION_PORTRAIT));
                    intent.putExtra(TransparentActivity.ORIENTATION, _cly.context_.getResources().getConfiguration().orientation);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    _cly.context_.startActivity(intent);
                }
            } catch (Exception ex) {
                L.e("[ModuleContent] fetchContentsInternal, Encountered internal issue while trying to fetch contents, [" + ex + "]");
            }
        }, L);
    }

    void registerForContentUpdates(String[] categories) {
        if (deviceIdProvider.isTemporaryIdEnabled()) {
            L.w("[ModuleContent] registerForContentUpdates, temporary device ID is enabled, skipping");
            return;
        }

        if (!shouldFetchContents) {
            L.w("[ModuleContent] registerForContentUpdates, shouldFetchContents is false, skipping");
            return;
        }

        this.categories = categories;

        countlyTimer.startTimer(contentUpdateInterval, () -> {
            if (experimental) {
                L.v("[ModuleContent] registerForContentUpdates, experimental mode enabled, directly fetching contents");
                fetchContentsInternal(categories);
            } else if (requestQueueProvider.isRequestQueueEmpty()) {
                String requestData = requestQueueProvider.prepareEngagementQueueFetch();

                ConnectionProcessor cp = requestQueueProvider.createConnectionProcessor();
                final boolean networkingIsEnabled = cp.configProvider_.getNetworkingEnabled();

                iRGenerator.CreateImmediateRequestMaker().doWork(requestData, "/i/content/queue/check", cp, false, networkingIsEnabled, checkResponse -> {
                    L.d("[ModuleContent] registerForContentUpdates, processing fetched contents, received response is :[" + checkResponse + "]");
                    if (checkResponse == null) {
                        return;
                    }

                    try {
                        validateAndCallFetch(checkResponse);
                    } catch (Exception ex) {
                        L.e("[ModuleContent] registerForContentUpdates, Encountered internal issue while trying to fetch contents, [" + ex + "]");
                    }
                }, L);
            } else {
                L.d("[ModuleContent] registerForContentUpdates, request queue is not empty, skipping");
                synchronized (this) {
                    shouldAddParamsToRequest = true;
                    requestQueueProvider.tick();
                }
            }
        }, L);
    }

    @NonNull
    private String prepareContentFetchRequest(@NonNull DisplayMetrics displayMetrics) {
        Resources resources = _cly.context_.getResources();
        int currentOrientation = resources.getConfiguration().orientation;
        boolean portrait = currentOrientation == Configuration.ORIENTATION_PORTRAIT;
        int navbarHeightScaled = 0;

        if (!ViewConfiguration.get(_cly.context_).hasPermanentMenuKey()) {
            int navbarHeightId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
            if (navbarHeightId > 0) {
                //disable this for now, as it is not working correctly
                navbarHeightScaled = (int) Math.ceil(resources.getDimensionPixelSize(navbarHeightId) / displayMetrics.density);
            }
        }

        int scaledWidth = (int) Math.ceil(displayMetrics.widthPixels / displayMetrics.density);
        int scaledHeight = (int) Math.ceil(displayMetrics.heightPixels / displayMetrics.density);

        int portraitWidth = portrait ? scaledWidth : scaledHeight;
        int portraitHeight = (portrait ? scaledHeight : scaledWidth);
        int landscapeWidth = portrait ? scaledHeight : scaledWidth;
        int landscapeHeight = portrait ? scaledWidth : scaledHeight;

        return requestQueueProvider.prepareFetchContents(portraitWidth, portraitHeight, landscapeWidth, landscapeHeight, categories);
    }

    boolean validateResponse(@NonNull JSONObject response) {
        boolean success = response.optBoolean("result", false);
        JSONArray hasContent = response.optJSONArray("content");
        return success && hasContent != null && hasContent.length() > 0;
    }

    void validateAndCallFetch(JSONObject response) {
        String checksum = response.optString("checksum", null);
        if (!checksum.equals(currentContentChecksum)) {
            currentContentChecksum = checksum;
            if (categories != null) {
                fetchContentsInternal(categories);
            }
        }
    }

    @NonNull
    Map<Integer, TransparentActivityConfig> parseContent(@NonNull JSONObject response, @NonNull DisplayMetrics displayMetrics) throws JSONException {
        Map<Integer, TransparentActivityConfig> placementCoordinates = new ConcurrentHashMap<>();
        JSONArray contents = response.optJSONArray("content");
        assert contents != null;

        JSONObject contentObj = contents.optJSONObject(0);
        assert contentObj != null;

        String content = contentObj.optString("pathToHtml");

        String contentChecksum = UtilsNetworking.sha256Hash(content); // TODO store this to prevent showing the same content again
        L.d("[ModuleContent] parseContent, checksum: [" + contentChecksum + "], current checksum: [" + currentContentChecksum + "]");

        if (!experimental && (currentContentChecksum != null && currentContentChecksum.equals(contentChecksum))) {
            L.d("[ModuleContent] parseContent, content did not change, skipping");
            return placementCoordinates;
        }
        currentContentChecksum = contentChecksum;

        JSONObject coordinates = contentObj.optJSONObject("placementCoordinates");

        assert coordinates != null;
        placementCoordinates.put(Configuration.ORIENTATION_PORTRAIT, extractOrientationPlacements(coordinates, displayMetrics.density, "portrait", content));
        placementCoordinates.put(Configuration.ORIENTATION_LANDSCAPE, extractOrientationPlacements(coordinates, displayMetrics.density, "landscape", content));

        return placementCoordinates;
    }

    private TransparentActivityConfig extractOrientationPlacements(@NonNull JSONObject placements, float density, @NonNull String orientation, @NonNull String content) {
        if (placements.has(orientation)) {
            JSONObject orientationPlacements = placements.optJSONObject(orientation);
            assert orientationPlacements != null;
            int x = orientationPlacements.optInt("x");
            int y = orientationPlacements.optInt("y");
            int w = orientationPlacements.optInt("width");
            int h = orientationPlacements.optInt("height");
            L.d("[ModuleContent] extractOrientationPlacements, orientation: [" + orientation + "], x: [" + x + "], y: [" + y + "], w: [" + w + "], h: [" + h + "]");
            System.err.println("[ModuleContent] extractOrientationPlacements, without ceil " + y * density);
            System.err.println("[ModuleContent] extractOrientationPlacements, with ceil " + (int) Math.ceil(y * density));

            TransparentActivityConfig config = new TransparentActivityConfig((int) Math.ceil(x * density), (int) Math.ceil(y * density), (int) Math.ceil(w * density), (int) Math.ceil(h * density));
            config.url = content;
            config.globalContentCallback = globalContentCallback;
            return config;
        }

        return null;
    }

    @Override
    void halt() {
        currentContentChecksum = null;
        contentInterface = null;
        countlyTimer.stopTimer(L);
        countlyTimer = null;
    }

    private void optOutFromContent() {
        exitFromContentInternal();
        shouldFetchContents = false;
    }

    @Override
    void onConsentChanged(@NonNull final List<String> consentChangeDelta, final boolean newConsent, @NonNull final ModuleConsent.ConsentChangeSource changeSource) {
        L.d("[ModuleContent] onConsentChanged, consentChangeDelta:[" + consentChangeDelta + "], newConsent:[" + newConsent + "], changeSource:[" + changeSource + "]");
        if (!experimental && consentChangeDelta.contains(Countly.CountlyFeatureNames.content) && !newConsent) {
            optOutFromContent();
        }
    }

    @Override
    void deviceIdChanged(boolean withoutMerge) {
        L.d("[ModuleContent] deviceIdChanged, withoutMerge:[" + withoutMerge + "]");
        if (withoutMerge) {
            optOutFromContent();
        }
    }

    @Override
    void onRequest(@NonNull StringBuilder request) {
        L.d("[ModuleContent] onRequest, request:[" + request + "]");
        if (experimental) {
            L.v("[ModuleContent] onRequest, experimental mode enabled, skipping");
            return;
        }
        synchronized (this) {
            if (shouldAddParamsToRequest) {
                request.append("&trigger=content-update");
            }
        }
    }

    @Override
    void onResponse(@NonNull JSONObject response) {
        L.d("[ModuleContent] onResponse, response:[" + response + "]");
        if (experimental) {
            L.v("[ModuleContent] onResponse, experimental mode enabled, skipping");
            return;
        }
        synchronized (this) {
            if (shouldAddParamsToRequest) {
                shouldAddParamsToRequest = false;
                validateAndCallFetch(response);
            }
        }
    }

    @Override
    void initFinished(@NotNull CountlyConfig config) {
        registerForContentUpdates(new String[] {});
    }

    protected void exitFromContentInternal() {
        countlyTimer.stopTimer(L);
    }

    public class Content {

        /**
         * Opt in user for the content fetching and updates
         *
         * @param categories categories for the content
         */
        // TODO this is an experimental for now, will not expose it to the public API
        protected void openForContent(@NonNull String... categories) {
            L.d("[ModuleContent] openForContent, categories: [" + Arrays.toString(categories) + "]");

            if (!experimental && !consentProvider.getConsent(Countly.CountlyFeatureNames.content)) {
                L.w("[ModuleContent] openForContent, Consent is not granted, skipping");
                return;
            }

            shouldFetchContents = true;
            registerForContentUpdates(categories);
        }

        /**
         * Opt in user for the content fetching and updates
         * <p>
         * <strong>Important Note:</strong> This method is an experimental feature and it might be removed in the future.
         * </p>
         */
        public void openForContent() {
            openForContent(new String[] {});
        }

        /**
         * Opt out user from the content fetching and updates
         * <p>
         * <strong>Important Note:</strong> This method is an experimental feature and it might be removed in the future.
         * </p>
         */
        public void exitFromContent() {
            if (!experimental && !consentProvider.getConsent(Countly.CountlyFeatureNames.content)) {
                L.w("[ModuleContent] openForContent, Consent is not granted, skipping");
                return;
            }

            exitFromContentInternal();
        }

        /**
         * Change the content that is being shown
         *
         * @param categories categories for the content
         */
        // TODO this is an experimental for now, will not expose it to the public API
        protected void changeContent(@NonNull String... categories) {
            L.d("[ModuleContent] changeContent, categories: [" + Arrays.toString(categories) + "]");

            if (!experimental && !consentProvider.getConsent(Countly.CountlyFeatureNames.content)) {
                L.w("[ModuleContent] openForContent, Consent is not granted, skipping");
                return;
            }

            registerForContentUpdates(categories);
        }

        /**
         * Change the content that is being shown
         */
        public void fetchContents() {
            fetchContentsInternal(new String[] { "" });
        }
    }
}
