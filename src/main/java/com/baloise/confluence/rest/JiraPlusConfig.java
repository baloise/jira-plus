package com.baloise.confluence.rest;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;

@Path("/")
public class JiraPlusConfig {

	private final UserManager userManager;
	private final PluginSettingsFactory pluginSettingsFactory;
	private final TransactionTemplate transactionTemplate;

	public JiraPlusConfig(UserManager userManager, PluginSettingsFactory pluginSettingsFactory, TransactionTemplate transactionTemplate) {
		this.userManager = userManager;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
	}

	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response get(@Context HttpServletRequest request)
	{
	  String username = userManager.getRemoteUsername(request);
	  if (username == null || !userManager.isSystemAdmin(username))
	  {
	    return Response.status(Status.UNAUTHORIZED).build();
	  }
	 
	  return Response.ok(loadConfig(transactionTemplate, pluginSettingsFactory)).build();
	}

	public static JiraPlusConfigModel loadConfig(final TransactionTemplate transactionTemplate, final PluginSettingsFactory pluginSettingsFactory ) {
		return (JiraPlusConfigModel) transactionTemplate.execute(new TransactionCallback()
		  {
		    public Object doInTransaction()
		    {
		      PluginSettings settings = pluginSettingsFactory.createGlobalSettings();
		      JiraPlusConfigModel config = new JiraPlusConfigModel();
		      config.setName((String) settings.get(JiraPlusConfigModel.class.getName() + ".name"));
		                 
		      String time = (String) settings.get(JiraPlusConfigModel.class.getName() + ".time");
		      if (time != null)
		      {
		        config.setTime(Integer.parseInt(time));
		      }
		      return config;
		    }
		  });
	}
	
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response put(final JiraPlusConfigModel config, @Context HttpServletRequest request)
	{
	  String username = userManager.getRemoteUsername(request);
	  if (username == null || !userManager.isSystemAdmin(username))
	  {
	    return Response.status(Status.UNAUTHORIZED).build();
	  }
	 
	  transactionTemplate.execute(new TransactionCallback()
	  {
	    public Object doInTransaction()
	    {
	      PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
	      pluginSettings.put(JiraPlusConfigModel.class.getName() + ".name", config.getName());
	      pluginSettings.put(JiraPlusConfigModel.class.getName()  +".time", Integer.toString(config.getTime()));
	      return null;
	    }
	  });
	  return Response.noContent().build();
	}
	
}