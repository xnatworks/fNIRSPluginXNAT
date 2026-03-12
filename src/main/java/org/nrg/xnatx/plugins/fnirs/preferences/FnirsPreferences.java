package org.nrg.xnatx.plugins.fnirs.preferences;

import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.configuration.ConfigPaths;
import org.nrg.framework.services.NrgEventServiceI;
import org.nrg.framework.utilities.OrderedProperties;
import org.nrg.prefs.annotations.NrgPreference;
import org.nrg.prefs.annotations.NrgPreferenceBean;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.prefs.services.NrgPreferenceService;
import org.nrg.xdat.preferences.EventTriggeringAbstractPreferenceBean;
import org.springframework.beans.factory.annotation.Autowired;

@NrgPreferenceBean(toolId = FnirsPreferences.FNIRS_TOOL_ID,
                   toolName = "XNAT fNIRS Plugin Preferences",
                   description = "Manages preferences and settings for the XNAT fNIRS plugin.")
@Slf4j
public class FnirsPreferences extends EventTriggeringAbstractPreferenceBean {
    public static final String FNIRS_TOOL_ID                    = "fnirsPlugin";
    public static final String INVALID_PREFERENCE_NAME_TEMPLATE = "Invalid preference name {}: something is very wrong here.";
    public static final String FNIRS_SUBJECT_TEMPLATE           = "fnirsSubjectId";
    public static final String FNIRS_SESSION_TEMPLATE           = "fnirsSessionId";
    public static final String USE_FNIRS_TEMPLATES_AS_REGEX     = "useFnirsTemplatesAsRegex";

    @Autowired
    public FnirsPreferences(final NrgPreferenceService preferenceService, final NrgEventServiceI eventService, final ConfigPaths configPaths, final OrderedProperties initPrefs) {
        super(preferenceService, eventService, configPaths, initPrefs);
    }

    @NrgPreference(defaultValue = "sub-")
    public String getFnirsSubjectId() {
        return getValue(FNIRS_SUBJECT_TEMPLATE);
    }

    @SuppressWarnings("unused")
    public void setFnirsSubjectId(final String fnirsSubjectId) {
        safeSet(fnirsSubjectId, FNIRS_SUBJECT_TEMPLATE);
    }

    @NrgPreference(defaultValue = "ses-")
    public String getFnirsSessionId() {
        return getValue(FNIRS_SESSION_TEMPLATE);
    }

    @SuppressWarnings("unused")
    public void setFnirsSessionId(final String fnirsSessionId) {
        safeSet(fnirsSessionId, FNIRS_SESSION_TEMPLATE);
    }

    @NrgPreference(defaultValue = "false")
    public boolean getUseFnirsTemplatesAsRegex() {
        return getBooleanValue(USE_FNIRS_TEMPLATES_AS_REGEX);
    }

    @SuppressWarnings("unused")
    public void setUseFnirsTemplatesAsRegex(final boolean useFnirsTemplatesAsRegex) {
        safeSet(useFnirsTemplatesAsRegex, USE_FNIRS_TEMPLATES_AS_REGEX);
    }

    private void safeSet(final String value, final String name) {
        try {
            set(value, name);
        } catch (InvalidPreferenceName e) {
            log.error(INVALID_PREFERENCE_NAME_TEMPLATE, name, e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void safeSet(final boolean value, final String name) {
        try {
            setBooleanValue(value, name);
        } catch (InvalidPreferenceName e) {
            log.error(INVALID_PREFERENCE_NAME_TEMPLATE, name, e);
        }
    }
}
