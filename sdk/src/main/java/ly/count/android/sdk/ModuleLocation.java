package ly.count.android.sdk;

public class ModuleLocation extends ModuleBase {

    boolean locationDisabled = false;
    String locationCountryCode = null;
    String locationCity = null;
    String locationGpsCoordinates = null;
    String locationIpAddress = null;

    Location locationInterface = null;

    boolean sendLocationPostInit;
    boolean postInitReached = false;//todo this looks like something that can be removed

    ModuleLog L;

    ModuleLocation(Countly cly, CountlyConfig config) {
        super(cly);

        L = cly.L;

        L.v("[ModuleLocation] Initialising");

        locationInterface = new ModuleLocation.Location();
    }

    void resetLocationValues() {
        locationCity = null;
        locationCountryCode = null;
        locationGpsCoordinates = null;
        locationIpAddress = null;
    }

    boolean anyValidLocation() {
        L.d("[ModuleLocation] Calling 'anyValidLocation'");

        if (locationDisabled) {
            return false;
        }

        if (locationCountryCode != null || locationCity != null || locationIpAddress != null || locationGpsCoordinates != null) {
            return true;
        }

        return false;
    }

    void sendCurrentLocation() {
        L.d("[ModuleLocation] Calling 'sendCurrentLocation'");
        _cly.connectionQueue_.sendLocation(locationDisabled, locationCountryCode, locationCity, locationGpsCoordinates, locationIpAddress);
    }

    void disableLocationInternal() {
        L.d("[ModuleLocation] Calling 'disableLocationInternal'");

        if (!_cly.getConsent(Countly.CountlyFeatureNames.location)) {
            //can't send disable location request if no consent given
            return;
        }

        resetLocationValues();
        locationDisabled = true;
        _cly.connectionQueue_.sendLocation(true, null, null, null, null);
    }

    void setLocationInternal(String country_code, String city, String gpsCoordinates, String ipAddress) {
        L.d("[ModuleLocation] Calling 'setLocationInternal'");

        L.d("[ModuleLocation] Setting location parameters, cc[" + country_code + "] cy[" + city + "] gps[" + gpsCoordinates + "] ip[" + ipAddress + "]");

        if (!_cly.getConsent(Countly.CountlyFeatureNames.location)) {
            return;
        }

        locationCountryCode = country_code;
        locationCity = city;
        locationGpsCoordinates = gpsCoordinates;
        locationIpAddress = ipAddress;

        if ((country_code == null && city != null) || (city == null && country_code != null)) {
            L.w("[ModuleLocation] In \"setLocation\" both city and country code need to be set at the same time to be sent");
        }

        if (country_code != null || city != null || gpsCoordinates != null || ipAddress != null) {
            locationDisabled = false;
        }

        if (_cly.isBeginSessionSent || !Countly.sharedInstance().getConsent(Countly.CountlyFeatureNames.sessions)) {
            //send as a separate request if either begin session was already send and we missed our first opportunity
            //or if consent for sessions is not given and our only option to send this is as a separate request
            if (postInitReached) {
                _cly.connectionQueue_.sendLocation(locationDisabled, locationCountryCode, locationCity, locationGpsCoordinates, locationIpAddress);
            } else {
                //if we are still in init, send it at the end so that the SDK finished initialisation
                sendLocationPostInit = true;
            }
        } else {
            //will be sent a part of begin session
        }
    }

    @Override
    void initFinished(CountlyConfig config) {
        //do location related things
        if (config.disableLocation) {
            locationDisabled = true;
            disableLocationInternal();
        } else {
            //if we are not disabling location, check for other set values
            if (config.locationIpAddress != null || config.locationLocation != null || config.locationCity != null || config.locationCountyCode != null) {
                setLocationInternal(config.locationCountyCode, config.locationCity, config.locationLocation, config.locationIpAddress);
            }
        }

        postInitReached = true;
        if (sendLocationPostInit) {
            L.d("[ModuleLocation] Sending location post init");
            _cly.connectionQueue_.sendLocation(locationDisabled, locationCountryCode, locationCity, locationGpsCoordinates, locationIpAddress);
        }
    }

    @Override
    void halt() {
        locationInterface = null;
    }

    public class Location {
        /**
         * Disable sending of location data. Erases server side saved location information
         */
        public void disableLocation() {
            synchronized (_cly) {
                L.i("[Location] Calling 'disableLocation'");

                disableLocationInternal();
            }
        }

        /**
         * Set location parameters. If they are set before begin_session, they will be sent as part of it.
         * If they are set after, then they will be sent as a separate request.
         * If this is called after disabling location, it will enable it.
         *
         * @param countryCode ISO Country code for the user's country
         * @param city Name of the user's city
         * @param gpsCoordinates comma separate lat and lng values. For example, "56.42345,123.45325"
         * @param ipAddress ipAddress like "192.168.88.33"
         * @return Returns link to Countly for call chaining
         */
        public void setLocation(String countryCode, String city, String gpsCoordinates, String ipAddress) {
            synchronized (_cly) {
                L.i("[Location] Calling 'setLocation'");

                setLocationInternal(countryCode, city, gpsCoordinates, ipAddress);
            }
        }
    }
}
