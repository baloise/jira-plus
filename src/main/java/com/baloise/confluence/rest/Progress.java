package com.baloise.confluence.rest;

import static java.awt.Color.BLACK;
import static java.awt.Color.decode;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.max;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

import javax.imageio.ImageIO;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Response.ResponseBuilder;

@Path("/progress")
public class Progress {
    
    @GET
    @AnonymousAllowed
	@Path("/{progress}")
	@Produces({"image/png"})
	public Response getUserImage(
			@DefaultValue("100") 		@QueryParam("width") 		final int width,
			@DefaultValue("20") 		@QueryParam("height") 		final int height,
			@DefaultValue("#FF9999") 	@QueryParam("todo") 		final String todo,
			@DefaultValue("#99FF99") 	@QueryParam("done") 		final String done,
			@DefaultValue("#000000") 	@QueryParam("border") 		final String border,
			@DefaultValue("true") 		@QueryParam("showLabel") 	final boolean showLabel,
			@DefaultValue("false") 		@QueryParam("showBorder") 	final boolean showBorder,
			@PathParam("progress") final int progress,
			@Context Request request
			) {
		  
		
			ResponseBuilder possibleResponse = request.evaluatePreconditions(new Date(0));
	        if (possibleResponse != null) {
	            return possibleResponse.build();
	        }
	        
			  CacheControl cc = new CacheControl();
		      final int fourteenDaysInSeconds = 60*60*24*14;
		      cc.setMaxAge(fourteenDaysInSeconds);
		      cc.setPrivate(false);
		      
		      return Response.ok()
		    		  .cacheControl(cc)
		    		  .lastModified(new Date()).entity(new StreamingOutput(){
		        
		    	public void write(OutputStream output) throws IOException, WebApplicationException {
	
					BufferedImage bi = createProgressImage(width, height, decode(todo), decode(done), progress, showLabel, showBorder, decode(border));
					ImageIO.write(bi, "png", output);
					output.flush();
		        }
	
		    }).build();
	}
 
	private BufferedImage createProgressImage(final int width, final int height, final Color todo, final Color done, final int progress, final boolean showLabel, final boolean showBorder, final Color border) {
		BufferedImage bi = new BufferedImage(width, height, TYPE_INT_ARGB);
		
		Graphics2D g2 = bi.createGraphics();
		final int dividerPos = width * progress/ 100;
		g2.setColor(done);
		g2.fillRect(0, 0, dividerPos, height);
		
		g2.setColor(todo);
		g2.fillRect(dividerPos, 0, width-dividerPos, height);
		
		if(showBorder) {
			g2.setColor(border);
			g2.drawRect(0, 0, width-1, height-1);
		}
		if(showLabel) {
			g2.setColor(BLACK);
			g2.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
			
			final int fontHeight = 10;
			final int fontWidth = 7;
			String label = progress+"%";
			g2.drawString(label, (width-(label.length()*fontWidth))/2, max((height-fontHeight )/2,0)+fontHeight);
		}
		
		return bi;
	}
}