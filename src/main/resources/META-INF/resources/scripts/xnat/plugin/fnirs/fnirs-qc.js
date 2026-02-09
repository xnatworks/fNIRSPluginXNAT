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

    XNAT.app.showFnirsQcImages = function(sessionId,scanId){
        let qcImageTypes = ["TT_dqc","SMI_dqc","Cap_dqc","nlrGray_dqc"];
        let qcImages = [];
        XNAT.xhr.getJSON({
            url: XNAT.url.restUrl('/data/experiments/'+sessionId+'/scans/'+scanId+'/resources/QC/files'),
            fail: function(e){ console.error(e) },
            success: function(data){
                qcImages=data.ResultSet.Result;
                if (qcImages.length > 0) {
                    qcImageTypes.forEach(function(type){
                        let img = qcImages.filter(function(file){ return file['Name'].indexOf(type) > 0 && file['Name'].indexOf('.png') > 0})[0];
                        if (img) {
                            $(document).find('.report-section.'+scanId + ' .snapshot-row').append(
                                spawn('.snapshot-container',{
                                    style: {
                                        'background-image':'url(' + XNAT.url.rootUrl(img['URI']) + ')',
                                        'margin-right':'1rem'
                                    },
                                    onclick: function(){ XNAT.ui.dialog.open({
                                        title: img['Name'],
                                        width: 800,
                                        content: '<img src="' + XNAT.url.rootUrl(img['URI']) + '" />',
                                        maxBtn: true,
                                        maxxed: true,
                                        buttons: [
                                            { label: 'OK', isDefault: true, close: true }
                                        ]
                                    })}
                                })
                            );
                        }
                    });
                } else {
                    let scanSection=$(document).find('.report-section.'+scanId);
                    scanSection.find('.snapshot-row').empty().append('No snapshots to view');
                    scanSection.find('.assessment').hide();
                    scanSection.find('input').prop('disabled','disabled');
                    scanSection.find('select').prop('disabled','disabled');
                }
            }
        });
    };

}));