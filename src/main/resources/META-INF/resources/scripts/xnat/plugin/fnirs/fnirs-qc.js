/*
 * fnirs-qc.js
 */

console.log('fnirs-qc.js');

var XNAT = getObject(XNAT || {});

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
    const x2js = new X2JS();

    XNAT.app.viewQcSnapshot = function(selectedImage,scanUrl,alternates){
        let footerContent = false,
            title = selectedImage['_name'];
        if (alternates.length){
            let options = '';
            alternates.forEach(function(file){
                let selected = (file['_name'] === selectedImage['_name']) ? 'selected' : '';
                options += '<option value="'+scanUrl+'files/'+file['_name']+'" '+selected+'>'+file['_name']+'</option>';
            });
            footerContent = 'Select Image: <select id="snapshotSelector">'+options+'</select>';
            title += ' (1 of '+alternates.length+')';
        }
        XNAT.ui.dialog.open({
            title: title,
            width: 800,
            content: '<img id="snapshotInView" src="' + XNAT.url.rootUrl(scanUrl + 'files/' + selectedImage['_URI']) + '" />',
            maxBtn: true,
            maxxed: true,
            footerContent: footerContent,
            buttons: [
                { label: 'OK', isDefault: true, close: true }
            ]
        })
    };

    $(document).on('change','#snapshotSelector',function(){
        var imgPath = XNAT.url.restUrl($(this).find('option:selected').val());
        $(document).find('#snapshotInView').prop('src',imgPath);
    });

    XNAT.app.showFnirsQcImages = function(sessionId,scanId){
        let qcImageTypes = ["TT_dqc","SMI_dqc","Cap_dqc","nlrGray_dqc","GLM_DM","thresholded_map","no_threshold_map"],
            scanUrl = '/data/experiments/'+sessionId+'/scans/'+scanId+'/resources/QC/';
        $(document).find('.panel.'+scanId + ' .snapshot-grid-container').html('Loading QC images for '+scanId);
        XNAT.xhr.get({
            url: XNAT.url.restUrl(scanUrl),
            async: true,
            fail: function(e){ console.error(e) },
            success: function(data){
                const imgContainer$ = $(document).find('.panel.'+scanId + ' .snapshot-grid-container');
                let rawData = x2js.xml2json(data);
                let qcImages=rawData.Catalog.entries.entry;
                if (qcImages.length > 0) {
                    imgContainer$.empty();
                    qcImageTypes.forEach(function(type){
                        let imgs = qcImages.filter(function(file){ return file['_name'].indexOf(type) > 0 }),
                            img, altImgs = [];
                        if (imgs.length){
                            // Check for multiple possible matches and filter by date to return the most recent
                            // Add other images as alternates that the user can select if desired
                            if (imgs.length > 1) {
                                img = imgs.sort(function(a,b){ return (a['_createdTime'] > b['_createdTime']) ? -1 : 1 })[0];
                                altImgs = imgs;
                            } else {
                                img = imgs[0];
                            }
                            imgContainer$.append(
                                spawn('div',{
                                    className: (imgs.length > 1) ? 'snapshot multiple' : 'snapshot',
                                    style: {
                                        'background-image':'url('+ XNAT.url.rootUrl(scanUrl + 'files/' + img['_URI']) + ')',
                                        'margin-right':'1rem'
                                    },
                                    onclick: function(){
                                        XNAT.app.viewQcSnapshot(img,scanUrl,altImgs);
                                    },
                                    data: {
                                        multiple: (imgs.length > 1) ? imgs.length : false
                                    }
                                })
                            );
                        }
                    });
                } else {
                    imgContainer$.html('No snapshots to view');
                    $(document).find('.panel.'+scanId+' .assessment').hide();
                    $(document).find('.panel.'+scanId+' input').prop('disabled','disabled');
                    $(document).find('.panel.'+scanId+' select').prop('disabled','disabled');
                }
            }
        });
    };
    
    XNAT.app.showFnirsQcMeasurements = function(sessionId,scanId){
        let qcFileUrl = '/data/experiments/'+sessionId+'/scans/'+scanId+'/resources/QC/files/DQ_metrics.json';
        XNAT.xhr.get({
            url: XNAT.url.restUrl(qcFileUrl),
            async: true,
            fail: function(e){
                if (e.status == 404) {
                    $(document).find('.'+scanId + '.fnirs-measurements').html('No measurements to view');
                } else { console.error(e); }
            },
            success: function(data){
                const measurementContainer$ = $(document).find('.'+scanId + '.fnirs-measurements');
                let measurementJson = JSON.parse(data);
                if (Object.keys(measurementJson).length > 0) {
                    for (let key in measurementJson){
                        let val = measurementJson[key];
                        if (!isNaN(val * 1)){
                            // convert string to number to access JS number functions
                            val = (val * 1).toPrecision(4);
                            val = (val > 0.01) ? val : (val * 1).toExponential(3);
                        }
                        measurementContainer$.append(XNAT.ui.panel.element({
                            label: escapeHTML(key),
                            html: escapeHTML(val)
                        }).element);
                    }
                } else {
                    measurementContainer$.html('No measurements to view');
                }
            }
        });
    };

}));