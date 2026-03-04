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

    XNAT.app.showFnirsQcImages = function(sessionId,scanId){
        let qcImageTypes = ["TT_dqc","SMI_dqc","Cap_dqc","nlrGray_dqc"],
            scanUrl = '/data/experiments/'+sessionId+'/scans/'+scanId+'/resources/QC/';
        $(document).find('.report-section.'+scanId + ' .snapshot-row').html('Loading QC images for '+scanId);
        XNAT.xhr.get({
            url: XNAT.url.restUrl(scanUrl),
            async: true,
            fail: function(e){ console.error(e) },
            success: function(data){
                const imgContainer$ = $(document).find('.report-section.'+scanId + ' .snapshot-row');
                let rawData = x2js.xml2json(data);
                let qcImages=rawData.Catalog.entries.entry;
                if (qcImages.length > 0) {
                    console.log('Found images in '+scanId);
                    imgContainer$.empty();
                    qcImageTypes.forEach(function(type){
                        let imgs = qcImages.filter(function(file){ return file['_name'].indexOf(type) > 0 && file['_name'].indexOf('.png') > 0});
                        if (imgs.length){
                            // check for multiple possible matches and filter by last modification date
                            let img = (imgs.length > 1) ?
                                imgs.sort(function(a,b){ return (a['_modifiedTime'] > b['_modifiedTime']) ? -1 : 1 })[0] :
                                imgs[0];
                            console.log(img['_name']);
                            imgContainer$.append(
                                spawn('.snapshot-container',{
                                    style: {
                                        'background-image':'url('+ XNAT.url.rootUrl(scanUrl + 'files/' + img['_URI']) + ')',
                                        'margin-right':'1rem'
                                    },
                                    onclick: function(){ XNAT.ui.dialog.open({
                                        title: img['_name'],
                                        width: 800,
                                        content: '<img src="' + XNAT.url.rootUrl(scanUrl + 'files/' + img['_URI']) + '" />',
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
                    imgContainer$.empty().html('No snapshots to view');
                    scanSection.find('.assessment').hide();
                    scanSection.find('input').prop('disabled','disabled');
                    scanSection.find('select').prop('disabled','disabled');
                }
            }
        });
    };

}));