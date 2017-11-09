package com.dotcms.rest.api.v1.workflow;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.repackage.com.google.common.annotations.VisibleForTesting;
import com.dotcms.repackage.javax.ws.rs.*;
import com.dotcms.repackage.javax.ws.rs.core.Context;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.glassfish.jersey.server.JSONP;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.WebResource;
import com.dotcms.rest.annotation.NoCache;
import com.dotcms.rest.exception.mapper.ExceptionMapperUtil;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotcms.workflow.helper.WorkflowHelper;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.Logger;
import com.google.common.annotations.Beta;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@SuppressWarnings("serial")
@Beta /* Non Official released */
@Path("/v1/workflow")
public class WorkflowResource {

    private final WorkflowHelper workflowHelper;
    private final WebResource webResource;

    /**
     * Default constructor.
     */
    public WorkflowResource() {
        this(WorkflowHelper.getInstance(), new WebResource());
    }

    @VisibleForTesting
    protected WorkflowResource(final WorkflowHelper workflowHelper,
                               final WebResource webResource) {

        this.workflowHelper = workflowHelper;
        this.webResource    = webResource;
    }


    @POST
    @Path("/action")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    @WrapInTransaction
    public final Response save(@Context final HttpServletRequest request,
                                         final WorkflowActionForm workflowActionForm) {

        this.webResource.init
                (null, true, request, true, null);
        Response response;
        WorkflowAction newAction;

        try {

            Logger.debug(this, "Saving new workflow action: " + workflowActionForm.getActionName());
            newAction = this.workflowHelper.save(workflowActionForm);
            response  = Response.ok(new ResponseEntityView(newAction)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(), e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // save

    @GET
    @Path("/actions/byscheme/{id}")
    @JSONP
    @NoCache
    @Produces({MediaType.APPLICATION_JSON, "application/javascript"})
    public final Response findActionsByScheme(@Context final HttpServletRequest request,
                                               @PathParam("id") String schemeId) {

        final InitDataObject initDataObject = this.webResource.init
                (null, true, request, true, null);
        Response response;
        List<WorkflowAction> actions;

        try {

            Logger.debug(this, "Getting the workflow actions: " + schemeId);
            actions   = this.workflowHelper.findActionsByScheme(schemeId, initDataObject.getUser());
            response  = Response.ok(new ResponseEntityView(actions)).build(); // 200
        } catch (Exception e) {

            Logger.error(this.getClass(), e.getMessage(), e);
            response = ExceptionMapperUtil.createResponse(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return response;
    } // findActionsByScheme.

} // E:O:F:WorkflowResource.
