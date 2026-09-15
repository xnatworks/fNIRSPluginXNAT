package org.nrg.xnatx.plugins.fnirs.preferences;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.prefs.exceptions.InvalidPreferenceName;
import org.nrg.xapi.rest.AbstractXapiRestController;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;

import static org.nrg.xdat.security.helpers.AccessLevel.DataAccess;
import static org.nrg.xdat.security.helpers.AccessLevel.DataAdmin;

@Api("XNAT fNIRS Preferences API")
@XapiRestController
@Slf4j
@RequestMapping("/fnirs")
public class FnirsPrefsApi extends AbstractXapiRestController {
    private final FnirsPreferences preferences;

    @Autowired
    public FnirsPrefsApi(final FnirsPreferences preferences, final UserManagementServiceI userManagementService, final RoleHolder roleHolder) {
        super(userManagementService, roleHolder);
        this.preferences = preferences;
    }

    @ApiOperation(value = "Returns the full map of fNIRS settings for this XNAT deployment.", notes = "Complex objects may be returned as encapsulated JSON strings.", response = String.class, responseContainer = "Map")
    @ApiResponses({@ApiResponse(code = 200, message = "DQR settings successfully retrieved."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Insufficient privileges to retrieve the requested setting."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "settings", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, restrictTo = DataAccess)
    public Map<String, Object> getFnirsPreferences() {
        log.info("User {} requested the system fNIRS settings.", getSessionUser().getUsername());
        return new HashMap<>(preferences);
    }

    @ApiOperation(value = "Sets a map of DQR properties.", notes = "Sets the DQR properties specified in the map.")
    @ApiResponses({@ApiResponse(code = 200, message = "Automation properties successfully set."),
                   @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT REST API."),
                   @ApiResponse(code = 403, message = "Not authorized to set automation properties."),
                   @ApiResponse(code = 500, message = "An unexpected error occurred.")})
    @XapiRequestMapping(value = "settings", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE}, method = RequestMethod.POST, restrictTo = DataAdmin)
    public void setDqrPreferences(@ApiParam(value = "The map of DQR preferences to be set.", required = true) @RequestBody final Map<String, String> properties) {
        log.info("User {} requested to set a batch of DQR preferences.", getSessionUser().getUsername());
        // Is this call initializing the system?
        for (final String name : properties.keySet()) {
            try {
                preferences.set(properties.get(name), name);
                log.info("Set property {} to value: {}", name, properties.get(name));
            } catch (InvalidPreferenceName invalidPreferenceName) {
                log.error("Got an invalid preference name error for the preference: {}, failed to set value to: {}", name, properties.get(name));
            }
        }
    }
}
