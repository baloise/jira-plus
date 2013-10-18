package com.baloise.confluence;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom.Element;
import org.jdom.filter.Filter;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.JiraIssuesManager.Channel;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.xhtml.api.XhtmlContent;

public class JiraPlusMacro implements Macro {

	private JiraIssuesManager jiraIssuesManager;
	private ApplicationLinkService applicationLinkService;

	public JiraPlusMacro(XhtmlContent xhtmlContent, JiraIssuesManager jiraIssuesManager, ApplicationLinkService applicationLinkService){
		this.jiraIssuesManager = jiraIssuesManager;
		this.applicationLinkService = applicationLinkService;
	}
	
	@Override
	public String execute(Map<String, String> parameters, String body, ConversionContext context) throws MacroExecutionException {
		
		String key = parameters.get("key");
        StringBuilder builder = new StringBuilder();
        builder.append("<p>");
        
        if(key==null) {
    		builder.append("must set a key");
        	builder.append("</p>");
            return builder.toString();
        } 
        
      
        Iterator<ApplicationLink> iterator = applicationLinkService.getApplicationLinks().iterator();
        ApplicationLink applink = null;
		while (applink == null && iterator.hasNext()) {
			ApplicationLink applicationLink = (ApplicationLink) iterator.next();
			if("applinks.jira".equals(applicationLink.getType().getI18nKey())){
				applink = applicationLink;
			}
		}
		
		if(applink==null) {
    		builder.append("No Jira linked: please contact the administrator");
        	builder.append("</p>");
            return builder.toString();
        } 
		
		List<String> columns = Arrays.asList("key", "summary");
		String url = buildKeyJiraUrl(key, applink);
		try {
			Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columns, applink, false, false);
			Element element = channel.getChannelElement();
			Iterator<Element> items = element.getDescendants(new Filter() {
				
				@Override
				public boolean matches(Object obj) {
					if (obj instanceof Element) {
						Element element = (Element) obj;
						if("item".equalsIgnoreCase(element.getName())){
							return true;
						}
					}
					return false;
				}
			});
			while (items.hasNext()) {
				Element item = items.next();
				builder.append("<a href='");
				builder.append(item.getChildText("link"));
				builder.append("'>");				
				builder.append(item.getChildText("key"));
				builder.append("</a>: ");
				builder.append(item.getChildText("summary"));
				builder.append("<br/>");
			}
			 
		} catch (Exception e) {
			e.printStackTrace();
			builder.append(e.getMessage());
		} 
		builder.append("</p>");
        return builder.toString();
	}

	@Override
	public BodyType getBodyType() {
		return BodyType.NONE;
	}

	@Override
	public OutputType getOutputType() {
		return OutputType.BLOCK;
	}
	
	private String buildKeyJiraUrl(String key, ApplicationLink applink)
    {
        String encodedQuery = utf8Encode("key in (" + key + ")");
        return normalizeUrl(applink.getRpcUrl())
                + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery="
                + encodedQuery;
    }
	
	public static String utf8Encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // will never happen in a standard java runtime environment
            throw new RuntimeException(
                    "You appear to not be running on a standard Java Runtime Environment");
        }
    }
	
	 private String normalizeUrl(URI rpcUrl) {
	        String baseUrl = rpcUrl.toString();
	        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	    }
	 
}
