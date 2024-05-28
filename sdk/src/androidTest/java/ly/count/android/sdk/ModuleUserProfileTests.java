package ly.count.android.sdk;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ModuleUserProfileTests {
    CountlyStore store;

    @Before
    public void setUp() {
        Countly.sharedInstance().halt();
        TestUtils.getCountyStore().clear();
    }

    @After
    public void tearDown() {
        TestUtils.getCountyStore().clear();
    }

    /**
     * Testing basic flow
     */
    @Test
    public void setAndSaveValues() throws JSONException {
        Countly mCountly = new Countly().init(TestUtils.createBaseConfig());

        Map<String, Object> userProperties = new ConcurrentHashMap<>();
        userProperties.put("name", "Test Test");
        userProperties.put("username", "test");
        userProperties.put("email", "test@gmail.com");
        userProperties.put("organization", "Tester");
        userProperties.put("phone", "+1234567890");
        userProperties.put("gender", "M");
        userProperties.put("picture", "http://domain.com/test.png");
        userProperties.put("byear", 2000);
        userProperties.put("key1", "value1");
        userProperties.put("key2", "value2");

        mCountly.userProfile().setProperties(userProperties);
        mCountly.userProfile().save();
        userProperties.remove("key1");
        userProperties.remove("key2");

        validateUserProfileRequest(userProperties, TestUtils.map("key1", "value1", "key2", "value2"));
    }

    /**
     * When recording an event, already not synced user profile data should be sent
     * before event sent, so event save will not be triggered
     */
    @Test
    public void SavingWritesEQIntoRQ() throws JSONException {
        Countly mCountly = new Countly().init(TestUtils.createBaseConfig());

        TestUtils.assertRQSize(0);
        mCountly.userProfile().setProperty("name", "Test Test");
        TestUtils.assertRQSize(0);

        mCountly.events().recordEvent("a");
        validateUserProfileRequest(TestUtils.map("name", "Test Test"), TestUtils.map());
    }

    // BELLOW TESTS THAT NEED TO BE REWORKED

    void assertAllValuesNull(ModuleUserProfile mup) {
        Assert.assertNull(mup.name);
        Assert.assertNull(mup.username);
        Assert.assertNull(mup.email);
        Assert.assertNull(mup.org);
        Assert.assertNull(mup.phone);
        Assert.assertNull(mup.gender);
        Assert.assertNull(mup.picture);
        Assert.assertEquals(0, mup.byear);
        Assert.assertNull(mup.custom);
        Assert.assertNull(mup.customMods);
    }

    void assertGivenCustomValues(Map<String, Object> data, ModuleUserProfile mup) {

        Assert.assertEquals(data.size(), mup.custom.size());

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();//todo rework to support more types

            Assert.assertEquals(value, mup.custom.get(key));
        }
    }

    void assertGivenValues(Map<String, Object> data, ModuleUserProfile mup) {

        if (data.containsKey("name")) {
            Assert.assertEquals(data.get("name"), mup.name);
        } else {
            Assert.assertNull(mup.name);
        }

        if (data.containsKey("username")) {
            Assert.assertEquals(data.get("username"), mup.username);
        } else {
            Assert.assertNull(mup.username);
        }

        if (data.containsKey("email")) {
            Assert.assertEquals(data.get("email"), mup.email);
        } else {
            Assert.assertNull(mup.email);
        }

        if (data.containsKey("organization")) {
            Assert.assertEquals(data.get("organization"), mup.org);
        } else {
            Assert.assertNull(mup.org);
        }

        if (data.containsKey("phone")) {
            Assert.assertEquals(data.get("phone"), mup.phone);
        } else {
            Assert.assertNull(mup.phone);
        }

        if (data.containsKey("picture")) {
            Assert.assertEquals(data.get("picture"), mup.picture);
        } else {
            Assert.assertNull(mup.picture);
        }

        if (data.containsKey("picturePath")) {
            Assert.assertEquals(data.get("picturePath"), mup.picturePath);
        } else {
            Assert.assertNull(mup.picturePath);
        }

        if (data.containsKey("gender")) {
            Assert.assertEquals(data.get("gender"), mup.gender);
        } else {
            Assert.assertNull(mup.gender);
        }

        if (data.containsKey("byear")) {
            Assert.assertEquals(Integer.parseInt((String) data.get("byear")), mup.byear);
        } else {
            Assert.assertEquals(0, mup.byear);
        }
    }

    HashMap<String, Object> createSetData_1() {
        Random rnd = new Random();
        HashMap<String, Object> data = new HashMap<>();
        data.put("name", "Test Test" + rnd.nextInt());
        data.put("username", "test" + rnd.nextInt());
        data.put("email", "test@gmail.com" + rnd.nextInt());
        data.put("organization", "Tester" + rnd.nextInt());
        data.put("phone", "+1234567890" + rnd.nextInt());
        data.put("gender", "M" + rnd.nextInt());
        data.put("picture", "http://domain.com/test.png" + rnd.nextInt());
        data.put("byear", "" + rnd.nextInt(100_000));

        return data;
    }

    HashMap<String, Object> createCustomSetData_1() {
        Random rnd = new Random();
        HashMap<String, Object> customdata = new HashMap<>();
        customdata.put("key" + rnd.nextInt(), "value" + rnd.nextInt());
        customdata.put("key" + rnd.nextInt(), "value2" + rnd.nextInt());
        customdata.put("key" + rnd.nextInt(), "value2" + rnd.nextInt());
        customdata.put("key" + rnd.nextInt(), "value2" + rnd.nextInt());
        customdata.put("key" + rnd.nextInt(), "value2" + rnd.nextInt());
        customdata.put("key" + rnd.nextInt(), "value2" + rnd.nextInt());

        return customdata;
    }

    @Test
    public void testSetData() {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        mCountly.init(config);

        HashMap<String, Object> data = new HashMap<>();
        data.put("name", "Test Test");
        data.put("username", "test");
        data.put("email", "test@gmail.com");
        data.put("organization", "Tester");
        data.put("phone", "+1234567890");
        data.put("gender", "M");
        data.put("picture", "http://domain.com/test.png");
        data.put("byear", "2000");
        data.put("key12", "value1");
        data.put("key22", "value2");
        mCountly.userProfile().setProperties(data);

        Assert.assertEquals("Test Test", mCountly.moduleUserProfile.name);
        Assert.assertEquals("test", mCountly.moduleUserProfile.username);
        Assert.assertEquals("test@gmail.com", mCountly.moduleUserProfile.email);
        Assert.assertEquals("Tester", mCountly.moduleUserProfile.org);
        Assert.assertEquals("+1234567890", mCountly.moduleUserProfile.phone);
        Assert.assertEquals("M", mCountly.moduleUserProfile.gender);
        Assert.assertEquals("http://domain.com/test.png", mCountly.moduleUserProfile.picture);
        Assert.assertEquals(2000, mCountly.moduleUserProfile.byear);
        Assert.assertEquals(false, mCountly.moduleUserProfile.isSynced);
        Assert.assertEquals(2, mCountly.moduleUserProfile.custom.size());
        Assert.assertEquals("value1", data.get("key12"));
        Assert.assertEquals("value2", data.get("key22"));
    }

    @Test
    public void testSetData_2() {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        mCountly.init(config);

        assertAllValuesNull(mCountly.moduleUserProfile);

        HashMap<String, Object> data = createSetData_1();
        HashMap<String, Object> customData = createCustomSetData_1();

        mCountly.userProfile().setProperties(data);
        mCountly.userProfile().setProperties(customData);

        assertGivenValues(data, mCountly.moduleUserProfile);
        assertGivenCustomValues(customData, mCountly.moduleUserProfile);
    }

    @Test
    public void testCustomData() {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        mCountly.init(config);

        HashMap<String, Object> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        mCountly.userProfile().setProperties(data);
        mCountly.userProfile().setProperty("key_prop", "value_prop");

        Assert.assertEquals("value1", mCountly.moduleUserProfile.custom.get("key1"));
        Assert.assertEquals("value2", mCountly.moduleUserProfile.custom.get("key2"));
        Assert.assertEquals("value_prop", mCountly.moduleUserProfile.custom.get("key_prop"));
    }

    @Test
    public void testCustomData_2() {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        mCountly.init(config);

        HashMap<String, Object> data = createCustomSetData_1();
        mCountly.userProfile().setProperties(data);

        assertGivenCustomValues(data, mCountly.moduleUserProfile);
    }

    @Test
    public void testCustomModifiers() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        mCountly.init(config);

        mCountly.moduleUserProfile.modifyCustomData("key_inc", 1, "$inc");
        mCountly.moduleUserProfile.modifyCustomData("key_mul", 2, "$mul");
        mCountly.moduleUserProfile.modifyCustomData("key_set", "test1", "$addToSet");
        mCountly.moduleUserProfile.modifyCustomData("key_set", "test2", "$addToSet");

        Assert.assertEquals(1, mCountly.moduleUserProfile.customMods.get("key_inc").getInt("$inc"));
        Assert.assertEquals(2, mCountly.moduleUserProfile.customMods.get("key_mul").getInt("$mul"));
        Assert.assertEquals("test1", mCountly.moduleUserProfile.customMods.get("key_set").getJSONArray("$addToSet").getString(0));
        Assert.assertEquals("test2", mCountly.moduleUserProfile.customMods.get("key_set").getJSONArray("$addToSet").getString(1));
    }

    @Test
    public void testClear() {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        mCountly.init(config);

        mCountly.userProfile().clear();
        assertAllValuesNull(mCountly.moduleUserProfile);

        HashMap<String, Object> data = createSetData_1();
        mCountly.userProfile().setProperties(data);
        assertGivenValues(data, mCountly.moduleUserProfile);

        mCountly.userProfile().clear();

        Assert.assertNull(mCountly.moduleUserProfile.name);
        Assert.assertNull(mCountly.moduleUserProfile.username);
        Assert.assertNull(mCountly.moduleUserProfile.email);
        Assert.assertNull(mCountly.moduleUserProfile.org);
        Assert.assertNull(mCountly.moduleUserProfile.phone);
        Assert.assertNull(mCountly.moduleUserProfile.gender);
        Assert.assertNull(mCountly.moduleUserProfile.picture);
        Assert.assertEquals(0, mCountly.moduleUserProfile.byear);
        Assert.assertNull(mCountly.moduleUserProfile.custom);
        Assert.assertNull(mCountly.moduleUserProfile.customMods);
    }

    @Test
    public void testJSON() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        mCountly.init(config);

        HashMap<String, Object> data = new HashMap<>();
        data.put("name", "Test Test");
        data.put("username", "test");
        data.put("email", "test@gmail.com");
        data.put("organization", "Tester");
        data.put("phone", "+1234567890");
        data.put("gender", "M");
        data.put("picture", "http://domain.com/test.png");
        data.put("byear", "2000");
        mCountly.userProfile().setProperties(data);

        HashMap<String, Object> customdata = new HashMap<>();
        customdata.put("key1", "value1");
        customdata.put("key2", "value2");
        mCountly.userProfile().setProperties(customdata);

        mCountly.userProfile().setProperty("key_prop", "value_prop");
        mCountly.moduleUserProfile.modifyCustomData("key_inc", 1, "$inc");
        mCountly.moduleUserProfile.modifyCustomData("key_mul", 2, "$mul");
        mCountly.moduleUserProfile.modifyCustomData("key_set", "test1", "$addToSet");
        mCountly.moduleUserProfile.modifyCustomData("key_set", "test2", "$addToSet");

        JSONObject json = mCountly.moduleUserProfile.toJSON();
        Assert.assertEquals("Test Test", json.getString("name"));
        Assert.assertEquals("test", json.getString("username"));
        Assert.assertEquals("test@gmail.com", json.getString("email"));
        Assert.assertEquals("Tester", json.getString("organization"));
        Assert.assertEquals("+1234567890", json.getString("phone"));
        Assert.assertEquals("M", json.getString("gender"));
        Assert.assertEquals("http://domain.com/test.png", json.getString("picture"));
        Assert.assertEquals(2000, json.getInt("byear"));
        Assert.assertEquals("value1", json.getJSONObject("custom").getString("key1"));
        Assert.assertEquals("value2", json.getJSONObject("custom").getString("key2"));
        Assert.assertEquals("value_prop", json.getJSONObject("custom").getString("key_prop"));
        Assert.assertEquals(1, json.getJSONObject("custom").getJSONObject("key_inc").getInt("$inc"));
        Assert.assertEquals(2, json.getJSONObject("custom").getJSONObject("key_mul").getInt("$mul"));
        Assert.assertEquals("test1", json.getJSONObject("custom").getJSONObject("key_set").getJSONArray("$addToSet").getString(0));
        Assert.assertEquals("test2", json.getJSONObject("custom").getJSONObject("key_set").getJSONArray("$addToSet").getString(1));
    }

    @Test
    public void testJSON_2() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        mCountly.init(config);

        assertAllValuesNull(mCountly.moduleUserProfile);

        HashMap<String, Object> data = createSetData_1();
        HashMap<String, Object> customData = createCustomSetData_1();

        mCountly.userProfile().setProperties(data);
        mCountly.userProfile().setProperties(customData);

        assertGivenValues(data, mCountly.moduleUserProfile);
        assertGivenCustomValues(customData, mCountly.moduleUserProfile);

        JSONObject json = mCountly.moduleUserProfile.toJSON();

        mCountly.userProfile().clear();
        assertAllValuesNull(mCountly.moduleUserProfile);

        mCountly.moduleUserProfile.fromJSON(json);

        assertGivenValues(data, mCountly.moduleUserProfile);
        assertGivenCustomValues(customData, mCountly.moduleUserProfile);
    }

    @Test
    public void testJSON_3() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        mCountly.init(config);

        assertAllValuesNull(mCountly.moduleUserProfile);

        JSONObject json = mCountly.moduleUserProfile.toJSON();

        mCountly.userProfile().clear();
        assertAllValuesNull(mCountly.moduleUserProfile);

        mCountly.moduleUserProfile.fromJSON(json);
        assertAllValuesNull(mCountly.moduleUserProfile);
    }

    @Test
    public void testGetDataForRequest() {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        mCountly.init(config);

        assertAllValuesNull(mCountly.moduleUserProfile);

        HashMap<String, Object> data = createSetData_1();
        HashMap<String, Object> customData = createCustomSetData_1();

        mCountly.userProfile().setProperties(data);
        mCountly.userProfile().setProperties(customData);

        String req = mCountly.moduleUserProfile.getDataForRequest();

        Assert.assertTrue(req.contains("&user_details="));
        Assert.assertTrue(req.contains("username"));
        Assert.assertTrue(req.contains("email"));
        Assert.assertTrue(req.contains("organization"));
        Assert.assertTrue(req.contains("picture"));
        Assert.assertTrue(req.contains("gender"));
        Assert.assertTrue(req.contains("custom"));
        Assert.assertTrue(req.contains("byear"));
    }

    /**
     * Test that custom data keys are truncated to the maximum allowed length (10)
     * Due to truncation, the keys "hair_color_id" and "hair_color_tone" will be merged into "hair_color"
     * The value of "hair_color" will be the value of "hair_color_tone" since it was set last
     * The value of "hair_skin_tone" will be truncated to "hair_skin_"
     * Tha last value of "hair_color" will be "black"
     * And predefined key "picturePath" is not truncated
     */
    @Test
    public void internalLimit_testCustomData() {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        config.sdkInternalLimits.setMaxKeyLength(10);
        mCountly.init(config);

        HashMap<String, Object> data = new HashMap<>();
        data.put("hair_color_id", 4567);
        data.put("hair_color_tone", "bold");
        mCountly.userProfile().setProperties(data);
        Assert.assertEquals(1, mCountly.moduleUserProfile.custom.size());
        Assert.assertEquals("bold", mCountly.moduleUserProfile.custom.get("hair_color"));

        mCountly.userProfile().setProperty("hair_color", "black");
        mCountly.userProfile().setProperty("hair_skin_tone", "yellow");
        mCountly.userProfile().setProperty("picturePath", "Test Test");
        Assert.assertEquals(2, mCountly.moduleUserProfile.custom.size());
        Assert.assertNull(ModuleUserProfile.picturePath);
        Assert.assertEquals("black", mCountly.moduleUserProfile.custom.get("hair_color"));
        Assert.assertEquals("yellow", mCountly.moduleUserProfile.custom.get("hair_skin_"));
    }

    /**
     * Test that custom data keys are truncated to the maximum allowed length (10)
     * Due to truncation, for push keys, the keys "reminder" and "rock" will be merged into same key
     */
    @Test
    public void internalLimit_testCustomModifiers() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(TestUtils.getContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        config.sdkInternalLimits.setMaxKeyLength(10);
        mCountly.init(config);

        mCountly.moduleUserProfile.modifyCustomData("key_inc_with", 1, "$inc");
        mCountly.moduleUserProfile.modifyCustomData("key_mul_width", 2, "$mul");
        mCountly.userProfile().push("key_push_reminder", "test1");
        mCountly.userProfile().push("key_push_rock", "test3");

        Assert.assertEquals(1, mCountly.moduleUserProfile.customMods.get("key_inc_wi").getInt("$inc"));
        Assert.assertEquals(2, mCountly.moduleUserProfile.customMods.get("key_mul_wi").getInt("$mul"));
        Assert.assertEquals(2, mCountly.moduleUserProfile.customMods.get("key_push_r").getJSONArray("$push").length());
        Assert.assertEquals("test1", mCountly.moduleUserProfile.customMods.get("key_push_r").getJSONArray("$push").getString(0));
        Assert.assertEquals("test3", mCountly.moduleUserProfile.customMods.get("key_push_r").getJSONArray("$push").getString(1));
    }

    /**
     * "setProperties" with both custom and predefined properties
     * custom properties should be truncated but predefined properties should not be truncated
     * validate that the predefined properties are not truncated
     */
    @Test
    public void internalLimit_setProperties() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = new CountlyConfig(ApplicationProvider.getApplicationContext(), "appkey", "http://test.count.ly").setDeviceId("1234").setLoggingEnabled(true).enableCrashReporting();
        config.sdkInternalLimits.setMaxKeyLength(2);
        mCountly.init(config);

        Countly.sharedInstance().userProfile().setProperties(TestUtils.map(
            ModuleUserProfile.BYEAR_KEY, 2000,
            ModuleUserProfile.EMAIL_KEY, "email",
            ModuleUserProfile.GENDER_KEY, "Male",
            ModuleUserProfile.PHONE_KEY, "phone",
            ModuleUserProfile.ORG_KEY, "org",
            ModuleUserProfile.USERNAME_KEY, "username",
            ModuleUserProfile.NAME_KEY, "name",
            ModuleUserProfile.PICTURE_KEY, "picture",
            "custom1", "value1",
            "custom2", 23,
            "hair", "black"
        ));
        Countly.sharedInstance().userProfile().save();

        validateUserProfileRequest(TestUtils.map(
                ModuleUserProfile.BYEAR_KEY, 2000,
                ModuleUserProfile.EMAIL_KEY, "email",
                ModuleUserProfile.GENDER_KEY, "Male",
                ModuleUserProfile.PHONE_KEY, "phone",
                ModuleUserProfile.ORG_KEY, "org",
                ModuleUserProfile.USERNAME_KEY, "username",
                ModuleUserProfile.NAME_KEY, "name",
                ModuleUserProfile.PICTURE_KEY, "picture"
            ), TestUtils.map(
                "cu", "23", // because in user profiles, all values are stored as strings
                "ha", "black")
        );
    }

    /**
     * Given max value size truncates the values of the:
     * - Custom user property values
     * - user property values except picture
     * Validate all values are truncated to the max value size that is 2
     * And validate non-String values are not clipped
     *
     * @throws JSONException if JSON parsing fails
     */
    @Test
    public void internalLimit_setProperties_maxValueSize() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = TestUtils.createBaseConfig();
        config.sdkInternalLimits.setMaxValueSize(2);
        mCountly.init(config);

        Object obj = new Object();
        Countly.sharedInstance().userProfile().setProperties(TestUtils.map(
            ModuleUserProfile.BYEAR_KEY, 2000,
            ModuleUserProfile.EMAIL_KEY, "email",
            ModuleUserProfile.GENDER_KEY, "Male",
            ModuleUserProfile.PHONE_KEY, "phone",
            ModuleUserProfile.ORG_KEY, "org",
            ModuleUserProfile.USERNAME_KEY, "username",
            ModuleUserProfile.NAME_KEY, "name",
            ModuleUserProfile.PICTURE_KEY, "picture",
            ModuleUserProfile.PICTURE_PATH_KEY, "TestTest",
            "custom1", "value1",
            "custom2", 23,
            "hair", "black",
            "custom3", 1234,
            "custom4", 1234.5,
            "custom5", true,
            "custom6", obj
        ));
        Countly.sharedInstance().userProfile().save();

        validateUserProfileRequest(TestUtils.map(
                ModuleUserProfile.BYEAR_KEY, 2000,
                ModuleUserProfile.EMAIL_KEY, "em",
                ModuleUserProfile.GENDER_KEY, "Ma",
                ModuleUserProfile.PHONE_KEY, "ph",
                ModuleUserProfile.ORG_KEY, "or",
                ModuleUserProfile.USERNAME_KEY, "us",
                ModuleUserProfile.NAME_KEY, "na",
                ModuleUserProfile.PICTURE_KEY, "picture"
            ), TestUtils.map(
                "custom1", "va", // because in user profiles, all values are stored as strings
                "custom2", "23",
                "hair", "bl",
                "custom3", "1234",
                "custom4", "1234.5",
                "custom5", "true",
                "custom6", obj.toString()) // toString() is called on non-String values
        );
    }

    /**
     * Given max value size for pictures 4096 truncates the value of the picture
     * and is not affected by the general max value size
     *
     * @throws JSONException if JSON parsing fails
     */
    @Test
    public void internalLimit_setProperties_maxValueSizePicture() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = TestUtils.createBaseConfig();
        config.sdkInternalLimits.setMaxValueSize(2);
        mCountly.init(config);

        String picture = TestUtils.generateRandomString(6000);

        Countly.sharedInstance().userProfile().setProperties(TestUtils.map(ModuleUserProfile.PICTURE_KEY, picture));
        Countly.sharedInstance().userProfile().save();

        validateUserProfileRequest(TestUtils.map(ModuleUserProfile.PICTURE_KEY, picture.substring(0, 4096)), TestUtils.map()
        );
    }

    /**
     * Given max segmentation values will truncate custom user properties to the correct length
     *
     * @throws JSONException if JSON parsing fails
     */
    @Test
    public void internalLimit_setProperties_maxSegmentationValues() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = TestUtils.createBaseConfig();
        config.sdkInternalLimits.setMaxSegmentationValues(2);
        mCountly.init(config);

        Countly.sharedInstance().userProfile().setProperties(TestUtils.map("a", "b", "c", "d", "f", 5, "level", 45, "age", 101));
        Countly.sharedInstance().userProfile().save();

        validateUserProfileRequest(TestUtils.map(), TestUtils.map("f", "5", "age", "101"));
    }

    /**
     * Given max segmentation values won't truncate custom mods
     *
     * @throws JSONException if JSON parsing fails
     */
    @Test
    public void internalLimit_customMods_maxSegmentationValues() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = TestUtils.createBaseConfig();
        config.sdkInternalLimits.setMaxSegmentationValues(2);
        mCountly.init(config);

        Countly.sharedInstance().userProfile().incrementBy("inc", 1);
        Countly.sharedInstance().userProfile().multiply("mul", 2_456_789);
        Countly.sharedInstance().userProfile().push("rem", "ORIELY");
        Countly.sharedInstance().userProfile().push("rem", "HUH");
        Countly.sharedInstance().userProfile().save();

        validateUserProfileRequest(TestUtils.map(), TestUtils.map("mul", json("$mul", 2_456_789), "rem", json("$push", new String[] { "ORIELY", "HUH" }), "inc", json("$inc", 1)));
    }

    /**
     * Given max value size truncates the values of the:
     * - Custom user property values
     * - user property values
     * Validate all values are truncated to the max value size that is 2
     * And validate non-String values are not clipped
     *
     * @throws JSONException if JSON parsing fails
     */
    @Test
    public void internalLimit_testCustomModifiers_setMaxValueSize() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        CountlyConfig config = TestUtils.createBaseConfig();
        config.sdkInternalLimits.setMaxValueSize(2);
        mCountly.init(config);

        mCountly.userProfile().incrementBy("inc", 1);
        mCountly.userProfile().multiply("mul", 2_456_789);
        mCountly.userProfile().push("rem", "ORIELY");
        mCountly.userProfile().push("rem", "HUH");
        mCountly.userProfile().pull("pll", "PULL");
        mCountly.userProfile().pushUnique("pshu", "PUSH");
        mCountly.userProfile().saveMax("sm", 455);
        mCountly.userProfile().saveMin("smi", 6789);
        mCountly.userProfile().setOnce("stc", "ONCE");

        Assert.assertEquals(1, mCountly.moduleUserProfile.customMods.get("inc").getInt("$inc"));
        Assert.assertEquals(2_456_789, mCountly.moduleUserProfile.customMods.get("mul").getInt("$mul"));
        Assert.assertEquals(2, mCountly.moduleUserProfile.customMods.get("rem").getJSONArray("$push").length());
        Assert.assertEquals("OR", mCountly.moduleUserProfile.customMods.get("rem").getJSONArray("$push").getString(0));
        Assert.assertEquals("HU", mCountly.moduleUserProfile.customMods.get("rem").getJSONArray("$push").getString(1));
        Assert.assertEquals("PU", mCountly.moduleUserProfile.customMods.get("pll").getString("$pull"));
        Assert.assertEquals("PU", mCountly.moduleUserProfile.customMods.get("pshu").getString("$addToSet"));
        Assert.assertEquals("455", mCountly.moduleUserProfile.customMods.get("sm").getString("$max"));
        Assert.assertEquals("6789", mCountly.moduleUserProfile.customMods.get("smi").getString("$min"));
        Assert.assertEquals("ON", mCountly.moduleUserProfile.customMods.get("stc").getString("$setOnce"));
    }

    /**
     * Validate that null value is eliminated from the user profile data
     *
     * @throws JSONException if JSON parsing fails
     */
    @Test
    public void setUserProperties_null() throws JSONException {
        Countly mCountly = Countly.sharedInstance();
        mCountly.init(TestUtils.createBaseConfig());

        HashMap<String, Object> data = new HashMap<>();
        data.put("null", null);

        mCountly.userProfile().setProperties(data);
        mCountly.userProfile().save();

        validateUserProfileRequest(new HashMap<>(), new HashMap<>());
    }

    /**
     * Related user properties should be saved before event recordings
     * call order, begin session, user property with "dark_mode", event, user property with "light_mode", end session
     * Manual sessions are enabled
     * generated request order  begin_session + first user property request + 3 events + user property request with light_mode + end_session
     */
    @Test
    public void eventSaveScenario_manualSessions() throws JSONException {
        Countly countly = new Countly().init(TestUtils.createBaseConfig().enableManualSessionControl());
        TestUtils.assertRQSize(0);

        countly.sessions().beginSession();
        TestUtils.assertRQSize(1); // begin session request
        countly.userProfile().setProperty("theme", "dark_mode");

        countly.events().recordEvent("test_event1");
        TestUtils.assertRQSize(2); // begin session request + user property request with dark_mode
        countly.events().recordEvent("test_event2");
        countly.events().recordEvent("test_event3");

        countly.userProfile().setProperty("theme", "light_mode");
        TestUtils.assertRQSize(2); // no request is generated on the way

        countly.sessions().endSession();
        // begin_session + first user property request + 3 events + user property request with light_mode + end_session
        validateUserProfileRequest(1, 5, TestUtils.map(), TestUtils.map("theme", "dark_mode"));
        validateUserProfileRequest(3, 5, TestUtils.map(), TestUtils.map("theme", "light_mode"));
    }

    /**
     * Related user properties should be saved before event recordings
     * call order, user property with "dark_mode", event, user property with "light_mode"
     * No consent for sessions
     * generated request order consent request + location request + first user property request + 3 events + user property request with light_mode
     */
    //@Test
    public void eventSaveScenario_onTimer() throws InterruptedException, JSONException {
        CountlyConfig config = TestUtils.createBaseConfig();
        config.sessionUpdateTimerDelay = 2; // trigger update call for property save
        Countly countly = new Countly().init(config);

        TestUtils.assertRQSize(2); // no begin session because of no consent
        //0 is the consent request
        TestUtils.validateRequest(TestUtils.commonDeviceId, TestUtils.map("location", ""), 1);

        countly.userProfile().setProperty("theme", "dark_mode");

        countly.events().recordEvent("test_event1");
        TestUtils.assertRQSize(3); // user property request with dark_mode
        countly.events().recordEvent("test_event2");
        countly.events().recordEvent("test_event3");

        countly.userProfile().setProperty("theme", "light_mode");
        TestUtils.assertRQSize(3); // no request is generated on the way

        Thread.sleep(2000);

        // first user property request + 3 events + user property request with light_mode
        validateUserProfileRequest(2, 5, TestUtils.map(), TestUtils.map("theme", "dark_mode"));
        validateUserProfileRequest(4, 5, TestUtils.map(), TestUtils.map("theme", "light_mode"));
    }

    /**
     * Related user properties should be saved before event recordings
     * call order, user property with "dark_mode", event, user property with "light_mode"
     * generated request order first user property request + 3 events + user property request with light_mode
     */
    @Test
    public void eventSaveScenario_changeDeviceIDWithoutMerge() throws JSONException {
        Countly countly = new Countly().init(TestUtils.createBaseConfig());

        TestUtils.assertRQSize(0);
        countly.userProfile().setProperty("theme", "dark_mode");

        countly.events().recordEvent("test_event1");
        TestUtils.assertRQSize(1); // user property request with dark_mode
        countly.events().recordEvent("test_event2");
        countly.events().recordEvent("test_event3");

        countly.userProfile().setProperty("theme", "light_mode");
        TestUtils.assertRQSize(1); // no request is generated on the way

        countly.deviceId().changeWithoutMerge("new_device_id");

        // first user property request + 3 events + user property request with light_mode
        validateUserProfileRequest(0, 3, TestUtils.map(), TestUtils.map("theme", "dark_mode"));
        validateUserProfileRequest(2, 3, TestUtils.map(), TestUtils.map("theme", "light_mode"));
    }

    private JSONObject json(Object... args) {
        return new JSONObject(TestUtils.map(args));
    }

    private void assertJsonsEquals(Object expected, Object actual) {
        Assert.assertEquals(expected.toString(), actual.toString());
    }

    private void validateUserProfileRequest(int idx, int size, Map<String, Object> predefined, Map<String, Object> custom) throws JSONException {
        Map<String, String>[] RQ = TestUtils.getCurrentRQ();
        Assert.assertEquals(size, RQ.length);
        JSONObject userDetails = new JSONObject(RQ[idx].get("user_details"));
        Assert.assertEquals(userDetails.length(), predefined.size() + 1);
        JSONObject customData = userDetails.getJSONObject("custom");
        Assert.assertEquals(customData.length(), custom.size());
        userDetails.remove("custom");
        for (Map.Entry<String, Object> entry : predefined.entrySet()) {
            Assert.assertEquals(entry.getValue(), userDetails.get(entry.getKey()));
        }

        for (Map.Entry<String, Object> entry : custom.entrySet()) {
            if (entry.getValue() instanceof JSONObject) {
                assertJsonsEquals(entry.getValue(), customData.get(entry.getKey()));
            } else {
                Assert.assertEquals(entry.getValue(), customData.get(entry.getKey()));
            }
        }
    }

    private void validateUserProfileRequest(Map<String, Object> predefined, Map<String, Object> custom) throws JSONException {
        validateUserProfileRequest(0, 1, predefined, custom);
    }
}
