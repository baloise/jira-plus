package com.baloise.confluence;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.jdom.Element;
import org.jdom.filter.Filter;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Renderer;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.JiraIssuesManager.Channel;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.baloise.confluence.rest.JiraPlusConfig;
import com.baloise.confluence.rest.JiraPlusConfigModel;

public class JiraPlusMacro implements Macro {

	private JiraIssuesManager jiraIssuesManager;
	private ApplicationLinkService applicationLinkService;
	private PluginSettingsFactory pluginSettingsFactory;
	private TransactionTemplate transactionTemplate;
	private Renderer renderer;
	private TemplateRenderer templateRenderer;
	private SettingsManager settingsManager;

	public JiraPlusMacro(XhtmlContent xhtmlContent, JiraIssuesManager jiraIssuesManager, ApplicationLinkService applicationLinkService, 
			PluginSettingsFactory pluginSettingsFactory, TransactionTemplate transactionTemplate, Renderer renderer, TemplateRenderer templateRenderer, 
			SettingsManager settingsManager){
		this.jiraIssuesManager = jiraIssuesManager;
		this.applicationLinkService = applicationLinkService;
		this.pluginSettingsFactory = pluginSettingsFactory;
		this.transactionTemplate = transactionTemplate;
		this.renderer = renderer;
		this.templateRenderer = templateRenderer;
		this.settingsManager = settingsManager;
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
        
        key = key.toUpperCase();
        
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
		
		List<String> columns = Arrays.asList("title", "status","summary", "issuelinks");
		String url = buildLinkedIssuesJiraUrl(key, applink);
		try {
			Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columns, applink, false, true);
			
			Map<String, Element> items = readItems(channel);
			builder.append(items.size()+" issues<br/>");
			Element issue = items.get(key);
			Map<String, Set<String>> links = getLinks(issue);
			builder.append("<b>");
			render(builder, issue);
			builder.append("</b>");
			for (String linkType : links.keySet()) {
				renderSection(builder, linkType);
				for (String linkedKey : links.get(linkType)) {
					render(builder, items.get(linkedKey));
				}
			}
			
			Map<String, Integer> data = new HashMap<String, Integer>();
			for (Entry<String, Set<String>> entry : links.entrySet()) {
				data.put(entry.getKey(), entry.getValue().size());
			}
			builder.append(pieChart("", data, context ));
			 
		} catch (Exception e) {
			e.printStackTrace();
			builder.append(e.getMessage());
		} 
		JiraPlusConfigModel config = JiraPlusConfig.loadConfig(transactionTemplate, pluginSettingsFactory);
		builder.append("<br/>Name from plugin config : "+config.getName());
		/*
		builder.append("<br/><progress value='50' max='70' ></progress>");
		builder.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" height=\"190\">\n" + 
				"   <polygon points=\"100,10 40,180 190,60 10,60 160,180\"\n" + 
				"   style=\"fill:lime;stroke:purple;stroke-width:5;fill-rule:evenodd;\">\n" + 
				"</svg>");
		*/
		Random rand = new Random(System.currentTimeMillis());
		int progress = rand.nextInt(50)+25;
		builder.append("<br/><img src='"+settingsManager.getGlobalSettings().getBaseUrl()+"/rest/progress/1.0/progress/"+progress+"' />");
		builder.append("</p>");
        return builder.toString();
	}

	
	String confluenceLink(String href, String text) {
		return "<ac:link><ri:page ri:content-title=\""+href+"\" /><ac:plain-text-link-body><![CDATA["+text+"]]></ac:plain-text-link-body></ac:link>";
	}
	
	String pieChart(String title, Map<String, Integer> data, ConversionContext conversionContext) throws IOException {
		StringWriter sw = new StringWriter();
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("title", title);
		context.put("data", data);
		templateRenderer.render("piechart.vm",context , sw);
		return renderer.render(sw.toString(), conversionContext);
	}
	
	private void renderSection(StringBuilder builder, String section) {
		builder.append("<i>");
		builder.append(section);
		builder.append("</i><br/>");
	}

	private void render(StringBuilder builder, Element item) {
		Element status = item.getChild("status");
		builder.append("<img src='");
		builder.append(status.getAttributeValue("iconUrl"));
		builder.append("' title='");				
		builder.append(status.getText());
		builder.append("'/> ");				
		
		builder.append("<a href='");
		builder.append(item.getChildText("link"));
		builder.append("'>");				
		builder.append(item.getChildText("key"));
		builder.append("</a>: ");
		builder.append(item.getChildText("summary"));
		builder.append("<br/>");
	}
	
	
	
	
	

	private Map<String, Element> readItems(Channel channel) {
		final Map<String, Element> ret = new HashMap<String, Element>();
		Iterator it = channel.getChannelElement().getDescendants(new Filter() {
			@Override
			public boolean matches(Object obj) {
				if (obj instanceof Element) {
					Element element = (Element) obj;
					if("item".equalsIgnoreCase(element.getName())){
						String key = element.getChildText("key");
						ret.put(key, element);
					}
				}
				return false;
			}
		});
		while (it.hasNext()) it.next();
		return ret;
	}
	
	private Map<String, Set<String>> getLinks(Element item) {
		final Map<String, Set<String>> ret = new HashMap<String, Set<String>>();
		if(item == null) return ret;
		Iterator it = item.getDescendants(new Filter() {
			@Override
			public boolean matches(Object obj) {
				if (obj instanceof Element) {
					Element element = (Element) obj;
					if("inwardlinks".equalsIgnoreCase(element.getName()) || 
						"outwardlinks".equalsIgnoreCase(element.getName())){
						String description = element.getAttributeValue("description");
						List<Element> links = element.getChildren("issuelink");
						Set<String> keySet = new HashSet<String>();
						for (Element link : links) {
							keySet.add(link.getChildText("issuekey"));
						}
						ret.put(description, keySet);
					}
				}
				return false;
			}
		});
		while (it.hasNext()) it.next();
		return ret;
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
        return buildJiraUrl("key in (" + key + ")", applink);
    }
	
	private String buildLinkedIssuesJiraUrl(String key, ApplicationLink applink)
    {
		return buildJiraUrl("issue in linkedIssuesFromQuery(\"issue in linkedIssuesFromQuery('key = " + key + "', 'Child', 'Outward', 3) or key = " + key + "\")", applink);
    }

	private String buildJiraUrl(String query, ApplicationLink applink) {
		return normalizeUrl(applink.getRpcUrl())
                + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery="
                + utf8Encode(query);
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
