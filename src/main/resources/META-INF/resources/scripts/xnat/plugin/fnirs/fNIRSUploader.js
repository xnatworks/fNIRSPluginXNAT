/*
 * web: fNIRSUploader.js
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

/*!
 * Used to upload fNIRS data to XNAT
 */

console.log('fNIRSUploader.js');

var XNAT = getObject(XNAT || {});
XNAT.plugin = getObject(XNAT.plugin || {});
XNAT.plugin.fnirs = getObject(XNAT.plugin.fnirs || {});

(function(factory) {
    if (typeof define === 'function' && define.amd) {
        define(factory);
    }
    else if (typeof exports === 'object') {
        module.exports = factory();
    }
    else {
        return factory();
    }
}(function() {

    var fnirsImporter;
    XNAT.plugin.fnirs.fnirsImporter = fnirsImporter = getObject(XNAT.plugin.fnirs.fnirsImporter || {});

    // Form Elements
    let formEl            = document.getElementById("upload-form");
    let projectEl         = document.getElementById("project");
    let prearchiveCode0El = document.getElementById("prearchive-code-0");
    let prearchiveCode1El = document.getElementById("prearchive-code-1");
    let uploadTypeEl      = document.getElementById("upload-type");
    let fileEl            = document.getElementById("file");
    let folderEl          = document.getElementById('folder');

    // Load projects then render project select box
    let getAllProjects = async function() {
        console.debug(`getAllProjects`);

        document.getElementById('upload-type').addEventListener('change', function() {
            let fileLabelEl = document.querySelector('.file-label');
            let folderLabelEl = document.querySelector('.folder-label');
            let fileNoteEl = document.getElementById('file-note');
            let folderNoteEl = document.getElementById('folder-note');
            let fileSection = document.getElementById('file-section');
            let folderSection = document.getElementById('folder-section');

            if (this.value === 'zip') {
                fileLabelEl.style.display = 'block';
                folderLabelEl.style.display = 'none';
                fileNoteEl.style.display = 'block';
                folderNoteEl.style.display = 'none';
                fileSection.style.display = 'block';
                folderSection.style.display = 'none';
                fileEl.required = true;
                folderEl.required = false;
            } else {
                fileLabelEl.style.display = 'none';
                folderLabelEl.style.display = 'block';
                fileNoteEl.style.display = 'none';
                folderNoteEl.style.display = 'block';
                fileSection.style.display = 'none';
                folderSection.style.display = 'block';
                fileEl.required = false;
                folderEl.required = true;
            }
        });

        let projectUrl = XNAT.url.restUrl('/data/projects');
        projectUrl = XNAT.url.addQueryString(projectUrl, ['prearc_code=true',
            'recent=true',
            'owner=true',
            'member=true',
            'collaborator=true']);

        const response = await fetch(projectUrl, {
            method: 'GET',
            headers: {'Content-Type': 'application/json'}
        })

        return response.json()
    }

    let renderProjectSelectBox = function(selectBox, projects) {
        // Clear select box
        selectBox.options.length = 0;

        // Placeholder
        selectBox.options[0] = new Option("Select " + XNAT.app.displayNames.singular.project, "");
        selectBox.options[0].disabled = true;
        selectBox.options[0].selected = true;

        projects.forEach(project => {
            selectBox.options[selectBox.length] = new Option(project['id'], project['id'])
        })
    }

    getAllProjects().then(resultSet => resultSet['ResultSet']["Result"]).then(projectsList =>{
        renderProjectSelectBox(projectEl, projectsList);
        projectEl.disabled = false;
    })

    fnirsImporter.performImport = async function(projectId, uploadId, filename, file) {
        let inputCachePath = `${uploadId}/${filename}`;
        let encodedCachePath = encodeURIComponent(inputCachePath);
        var importServiceUrl;
        if (uploadTypeEl.value === 'zip') {
            importServiceUrl = XNAT.url.csrfUrl('/xapi/fnirs/import/zip');
        } else {
            importServiceUrl = XNAT.url.csrfUrl('/xapi/fnirs/import/directory');
        }
        importServiceUrl = XNAT.url.addQueryString(importServiceUrl, [`projectId=${projectId}`,`cachePath=${encodedCachePath}`]);

        // Extract the zip file from the users cache
        console.debug(`Extracting ${file.name}`);
        XNAT.ui.dialog.static.wait(`Extracting ${file.name}`,{id: "fnirs_extraction"});

        response = await fetch(importServiceUrl, {method: 'POST'})

        if (response.ok) {
            console.debug('Extraction successful');
            XNAT.ui.dialog.close("fnirs_extraction");

            XNAT.ui.dialog.open({
                title: 'Upload/Extraction Successful',
                content: '<div class="success">Upload/Extraction of ' + file.name + ' successful. Visit the <a target="_blank" href="' + XNAT.url.rootUrl('/app/template/XDATScreen_prearchives.vm') + '">prearchive</a> to review.</div>',
                buttons: [
                    {
                        label: 'OK',
                        isDefault: true,
                        close: true,
                    }
                ]
            })
        } else {
            console.error(`Failed to extract ${file.name}`)
            XNAT.ui.dialog.close("fnirs_extraction");

            response.text().then(text => {
                // Error message is returned as html. Try to extract from the h3 tags.
                let error = text.match(/<h3>(.*)<\/h3>/);

                if (error.length === 2) {
                    return `Failed to extract ${file.name}: <br> <b>${error[1]}</b>`;
                } else {
                    return `Failed to extract ${file.name}`;
                }
            }).then(error => {
                XNAT.ui.dialog.open({
                    title: 'Extraction Failed',
                    content: `<div class="error">${error}</div>`,
                    buttons: [
                        {
                            label: 'OK',
                            isDefault: true,
                            close: true,
                        }
                    ]
                })
            })
        }
    }

    fnirsImporter.uploadFileToCache = async function(projectId, uploadId, cachePath, file) {
        let userResourceCacheUrl = XNAT.url.csrfUrl(`/data/${cachePath}`);

        let formDataFileOnly = new FormData();
        formDataFileOnly.append('file', file);

        // Upload the zip file to the users cache
        console.debug(`Uploading ${file.name}`);

        var response = await fetch(userResourceCacheUrl, {
                method: 'PUT',
                body: formDataFileOnly
            })

        if (response.ok) {
            console.debug('Upload successful');
            return true;
        } else {
            console.error(`Failed to upload ${file.name}`)

            XNAT.ui.dialog.open({
                title: 'Upload Failed',
                content: `<div class="error">Failed to upload ${file.name}.</div>`,
                buttons: [
                    {
                        label: 'OK',
                        isDefault: true,
                        close: true,
                    }
                ]
            })
            return false;
        }
    }

    fnirsImporter.uploadFnirsZip = async function(projectId, uploadId) {
        let file = fileEl.files[0];
        XNAT.ui.dialog.static.wait(`Uploading ${file.name}`,{id: "fnirs_upload"});
        let cachePath = `/user/cache/resources/${uploadId}/files/${file.name}`;
        await fnirsImporter.uploadFileToCache(projectId, uploadId, cachePath, file);
        XNAT.ui.dialog.close("fnirs_upload");
        let encodedFileName = encodeURIComponent(file.name);
        await fnirsImporter.performImport(projectId, uploadId, encodedFileName, file);
    }

    fnirsImporter.uploadFnirsFolder = async function(projectId, uploadId) {
        let filteredFiles = Array.from(folderEl.files).filter(file => !shouldExcludeFile(file.name));

        const listOfCreatedDirectories = new Array();

        let uploadDialog = XNAT.ui.dialog.static.wait('Uploading Directory: 0/' + filteredFiles.length + ' Complete', {id: "fnirs_upload"});
        for (let i = 0; i < filteredFiles.length; i++) {
            uploadDialog.update(spawn('div.message.waiting.md', `Uploading Directory: ${i + 1}/${filteredFiles.length} Complete`));
            const file = filteredFiles[i];
            let filePath = file.webkitRelativePath;
            let directory = filePath.substring(0, filePath.lastIndexOf("/"));
            if (!listOfCreatedDirectories.includes(directory)) {
                let directoryCachePath = `/user/cache/resources/${uploadId}/${directory}`;
                let successful_upload = await fnirsImporter.uploadFileToCache(projectId, uploadId, directoryCachePath, file);
                listOfCreatedDirectories.push(directory);
            }
            let cachePath = `/user/cache/resources/${uploadId}/files/${filePath}`;
            let successful_upload = await fnirsImporter.uploadFileToCache(projectId, uploadId, cachePath, file);
            if (!successful_upload) {
                XNAT.ui.dialog.close("fnirs_upload");
                return false;
            }
        }
        XNAT.ui.dialog.close("fnirs_upload");
        let relativeCachePath = folderEl.files[0].webkitRelativePath.split('/')[0];
        let encodedFileName = encodeURIComponent(relativeCachePath);
        await fnirsImporter.performImport(projectId, uploadId, encodedFileName, file);
        return true;
    }

    fnirsImporter.submitForm = function() {
        function validateForm() {
            let validProject = XNAT.validate(projectEl).required().check();
            let validImageArchiveFile = uploadTypeEl.value === 'zip'
                ? XNAT.validate(fileEl).is('fileType', 'zip').check()
                : folderEl.files.length > 0;

            return validProject && validImageArchiveFile;
        }

        let isValid = validateForm();
        if (!isValid) {
            return;
        }

        let projectId = projectEl.value;
        let uploadId = new Date().toISOString().replaceAll('-', '_').replaceAll(':', '_').replaceAll('.', '_');

        if (uploadTypeEl.value === 'zip') {
            fnirsImporter.uploadFnirsZip(projectId, uploadId);
        } else {
            fnirsImporter.uploadFnirsFolder(projectId, uploadId);
        }
    }
}));

function shouldExcludeFile(fileName) {
    const excludedFiles = ['Thumbs.db', 'desktop.ini'];
    return fileName.startsWith('.') || excludedFiles.includes(fileName);
}