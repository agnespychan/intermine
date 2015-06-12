package org.thalemine.web.domain;

import java.util.ArrayList;
import java.util.List;

import org.intermine.api.results.ResultElement;
import org.apache.log4j.Logger;

public class PublicationVO {

	protected static final Logger LOG = Logger.getLogger(PublicationVO.class);
	
	private String objectId;
	private String title;
	private String year;
	private String firstAuthor;
	private List<AuthorVO> authors = new ArrayList<AuthorVO>();
	
	public List<AuthorVO> getAuthors() {
		return authors;
	}
	public void setAuthors(List<AuthorVO> authors) {
		this.authors = authors;
	}
	public  PublicationVO(){
		
	}
	public String getObjectId() {
		return objectId;
	}
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getYear() {
		return year;
	}
	public void setYear(String year) {
		this.year = year;
	}
	public String getFirstAuthor() {
		return firstAuthor;
	}
	public void setFirstAuthor(String firstAuthor) {
		this.firstAuthor = firstAuthor;
	}
	
}
