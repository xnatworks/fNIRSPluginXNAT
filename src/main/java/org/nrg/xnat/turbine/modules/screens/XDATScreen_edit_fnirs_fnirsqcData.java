package org.nrg.xnat.turbine.modules.screens;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.model.FnirsFnirsscandataI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatQcscandataI;
import org.nrg.xdat.om.FnirsFnirsqcdata;
import org.nrg.xdat.om.FnirsFnirsqcscandata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrqcscandata;
import org.nrg.xdat.om.XnatMrscandata;
import org.nrg.xdat.om.XnatOtherqcscandata;
import org.nrg.xdat.om.XnatPetqcscandata;
import org.nrg.xdat.om.XnatPetscandata;
import org.nrg.xdat.om.FnirsFnirsscandata;
import org.nrg.xdat.om.XnatQcscandata;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.security.UserI;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

public class XDATScreen_edit_fnirs_fnirsqcData extends EditImageAssessorScreen {
    static Logger logger = Logger.getLogger(XDATScreen_edit_fnirs_fnirsqcData.class);

    public String getElementName() {
        return "fnirs:fnirsqcData";
    }

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.XDATScreen_edit_xnat_qcManualAssessorData#getEmptyItem(org.apache.turbine.util.RunData)
     */
    @Override
    public ItemI getEmptyItem(RunData data) throws Exception {
        final UserI user = TurbineUtils.getUser(data);
        final FnirsFnirsqcdata qcAccessor = new FnirsFnirsqcdata(XFTItem.NewItem(getElementName(), user));
        final String searchElement = TurbineUtils.GetSearchElement(data);
        if (!StringUtils.isEmpty(searchElement)) {
            final GenericWrapperElement se = GenericWrapperElement.GetElement(searchElement);
            if (se.instanceOf(XnatImagesessiondata.SCHEMA_ELEMENT_NAME)) {
                final String searchValue = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_value",data));
                if (!StringUtils.isEmpty(searchValue)) {
                    XnatImagesessiondata imageSession = new XnatImagesessiondata(TurbineUtils.GetItemBySearch(data));

                    populateDetails(qcAccessor,imageSession,user,data);
                }
            }
        }

        return qcAccessor.getItem();
    }

    /**
     * @param qcAccessor populate default qc assessor
     * @param imageSession session for qc
     * @param user for transaction
     * @throws Exception passed out from XFT transactions
     */
    private void populateDetails(final FnirsFnirsqcdata qcAccessor, final XnatImagesessiondata imageSession, final UserI user,final RunData data) throws Exception{
        qcAccessor.setImageSessionData(imageSession);

        // set defaults for new qc assessors
        if(StringUtils.isEmpty(qcAccessor.getImagesessionId())){
            qcAccessor.setImagesessionId(imageSession.getId());
        }

        if(StringUtils.isEmpty(qcAccessor.getId())){
            qcAccessor.setId(XnatExperimentdata.CreateNewID());
        }

        if(StringUtils.isEmpty(qcAccessor.getLabel())){
            qcAccessor.setLabel(imageSession.getLabel() + "_"+ Calendar.getInstance().getTimeInMillis());
        }


        if(StringUtils.isEmpty(qcAccessor.getProject())){
            qcAccessor.setProject(imageSession.getProject());
        }

        List<Object> types= Lists.newArrayList();
        if(TurbineUtils.HasPassedParameter("types", data)){
            //only show requested modalities
            types=Lists.newArrayList(Arrays.asList(TurbineUtils.GetPassedObjects("types", data)));
        }else if(qcAccessor.getScans_scan().size()>0){
            //show similar scans (by modality)
            for(final XnatQcscandataI scan: qcAccessor.getScans_scan()){
                if(!types.contains(scan.getXSIType())){
                    types.add(scan.getXSIType());
                }
            }
        }

        List<XnatImagescandata> imageScans = imageSession.getScans_scan();
        imageScans.sort(Comparator.comparing(XnatImagescandata::getId));
        for (XnatImagescandataI imageScan: imageScans){
            if(types.size()==0 || types.contains(imageScan.getXSIType())){
                XnatQcscandata scan = (XnatQcscandata)getQCScan(qcAccessor,imageScan.getId());
                if(scan==null){
                    if (FnirsFnirsscandata.SCHEMA_ELEMENT_NAME.equals(imageScan.getXSIType())) {
                        scan = new FnirsFnirsqcscandata(user);
                    }
                    scan.setImagescanId(imageScan.getId());
                    qcAccessor.setScans_scan(scan);
                }
            }
        }
    }

    /**
     * @param qc assessment
     * @param imageScanId id of scan to retrieve
     * @return matched scan
     */
    private XnatQcscandataI getQCScan(final FnirsFnirsqcdata qc, final String imageScanId){
        for(XnatQcscandataI s: qc.getScans_scan()){
            if(imageScanId.equals(s.getImagescanId())){
                return s;
            }
        }

        return null;
    }

    @Override
    public void finalProcessing(RunData data, Context context) {
        final FnirsFnirsqcdata qcAccessor=(FnirsFnirsqcdata)context.get("om");

        if(qcAccessor.getImageSessionData()!=null){
            try {
                populateDetails(qcAccessor,qcAccessor.getImageSessionData(),TurbineUtils.getUser(data),data);
            } catch (Exception e) {
                logger.error("",e);
            }
        }
    }
}
