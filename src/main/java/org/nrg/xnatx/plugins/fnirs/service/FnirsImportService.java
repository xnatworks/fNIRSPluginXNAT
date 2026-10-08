package org.nrg.xnatx.plugins.fnirs.service;

import org.nrg.xft.security.UserI;

import java.io.IOException;
import java.util.ArrayList;

public interface FnirsImportService {
    void importFnirsDataFromZip(String projectId, String cachePath, UserI user) throws Exception;
    void importFnirsDataFromFolder(String projectId, String cacheBasePath, UserI user) throws Exception;
}
