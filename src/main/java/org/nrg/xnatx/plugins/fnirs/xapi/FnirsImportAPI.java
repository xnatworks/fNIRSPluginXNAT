package org.nrg.xnatx.plugins.fnirs.xapi;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.action.ClientException;
import org.nrg.framework.annotations.XapiRestController;
import org.nrg.xapi.rest.AbstractXapiProjectRestController;
import org.nrg.xapi.rest.Project;
import org.nrg.xapi.rest.XapiRequestMapping;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.helpers.AccessLevel;
import org.nrg.xdat.security.services.RoleHolder;
import org.nrg.xdat.security.services.UserManagementServiceI;
import org.nrg.xft.utils.fileExtraction.Format;
import org.nrg.xnatx.plugins.fnirs.preferences.FnirsPreferences;
import org.nrg.xnatx.plugins.fnirs.service.FnirsImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.ArrayList;

@Api("fNIRS Import API")
@XapiRestController
@RequestMapping("/fnirs/import")
@Slf4j
public class FnirsImportAPI extends AbstractXapiProjectRestController {

    final FnirsPreferences preferences;
    final FnirsImportService importService;

    @Autowired
    public FnirsImportAPI(final RoleHolder roleHolder,
                          final UserManagementServiceI userManagementService,
                          final FnirsPreferences preferences,
                          final FnirsImportService importService) {
        super(userManagementService, roleHolder);
        this.preferences = preferences;
        this.importService = importService;
    }

    @ApiOperation(value = "Import fNIRS subjects and sessions from a zip archive.")
    @ApiResponses({@ApiResponse(code = 200, message = "fNIRS elements successfully created."),
            @ApiResponse(code = 401, message = "You must have sufficient permissions to add data to this project."),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST,
            value = "/zip", restrictTo = AccessLevel.Edit)
    public void createFnirsElementsFromZip(@RequestParam @Project String projectId,
                                           @RequestParam String cachePath) throws Exception {
        checkForImproperProjectId(projectId);

        if (Format.getFormat(cachePath) != Format.ZIP) {
            throw new ClientException("You must pass a zip archive in order to use this uploader.");
        }

        importService.importFnirsDataFromZip(projectId, cachePath, getSessionUser());
    }

    @ApiOperation(value = "Import fNIRS subjects and sessions from an input directory structure.")
    @ApiResponses({@ApiResponse(code = 200, message = "fNIRS elements successfully created."),
            @ApiResponse(code = 401, message = "You must have sufficient permissions to add data to this project."),
            @ApiResponse(code = 500, message = "Unexpected error")})
    @XapiRequestMapping(produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST,
            value = "/directory", restrictTo = AccessLevel.Edit)
    public void createFnirsElementsFromDirectory(@RequestParam @Project String projectId,
                                                 @RequestParam String cachePath) throws Exception {
        checkForImproperProjectId(projectId);

        importService.importFnirsDataFromFolder(projectId, cachePath, getSessionUser());
    }

    private void checkForImproperProjectId(String projectId) throws ClientException {
        if (StringUtils.isEmpty(projectId)) {
            throw new ClientException("Project Id blank or not present. This a required parameter for fNIRS uploads.");
        }

        XnatProjectdata project = XnatProjectdata.getXnatProjectdatasById(projectId, getSessionUser(), false);

        if (project == null) {
            throw new ClientException("No project with ID " + projectId + " exists.");
        }
    }
}
