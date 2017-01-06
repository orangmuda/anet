package mil.dds.anet.resources;

import java.util.List;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.dropwizard.auth.Auth;
import mil.dds.anet.AnetObjectEngine;
import mil.dds.anet.beans.Person;
import mil.dds.anet.beans.Position;
import mil.dds.anet.beans.Position.PositionType;
import mil.dds.anet.beans.search.PersonSearchQuery;
import mil.dds.anet.database.PersonDao;
import mil.dds.anet.graphql.GraphQLFetcher;
import mil.dds.anet.graphql.GraphQLParam;
import mil.dds.anet.graphql.IGraphQLResource;
import mil.dds.anet.utils.ResponseUtils;

@Path("/api/people")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class PersonResource implements IGraphQLResource {
	
	private PersonDao dao;
	
	public PersonResource(AnetObjectEngine engine) { 
		this.dao = engine.getPersonDao();
	}
	
	@Override
	public Class<Person> getBeanClass() { return Person.class; } 
	
	@Override
	public String getDescription() { return "People"; } 
	
	/**
	 * Returns all people objects in the ANET system. Does no filtering on role/status/etc. 
	 * @param pageNum 0 indexed page number of results to get. Defaults to 0. 
	 * @param pageSize Defaults to 100
	 * @return List of People objects in the system
	 */
	@GET
	@GraphQLFetcher
	@Path("/")
	public List<Person> getAll(@DefaultValue("0") @QueryParam("pageNum") int pageNum, @DefaultValue("100") @QueryParam("pageSize") int pageSize) {
		return dao.getAll(pageNum, pageSize);
	}
	
	/**
	 * Returns a single person entry based on ID. 
	 */
	@GET
	@Path("/{id}")
	@GraphQLFetcher
	@Produces(MediaType.APPLICATION_JSON)
	public Person getById(@PathParam("id") int id) { 
		Person p = dao.getById(id);
		if (p == null) { throw new WebApplicationException("No such person", Status.NOT_FOUND); }
		return p;
	}
	
	
	/**
	 * Creates a new {@link Person} object as supplied in http entity. 
	 * Optional: 
	 * - position: If you provide a Position ID number in the Position object, this person will be associated with that position (Potentially removing anybody currently in the position)
	 * @return the same Person object with the ID field filled in. 
	 */
	@POST
	@Path("/new")
	@RolesAllowed("SUPER_USER")
	public Person createNewPerson(Person p) {
		Person created = dao.insert(p);
		
		if (created.getPositionJson() != null) { 
			AnetObjectEngine.getInstance().getPositionDao().setPersonInPosition(created, created.getPositionJson());
		}
		
		return created;
	}
	
	/**
	 * Will update a person record with the {@link Person} entity provided in the http entity. All fields will be updated, so you must pass the complete Person object.
	 * Must be 
	 *   1) The person editing yourself
	 *   2) A super user for the person's organization
	 *   3) An administrator 
	 * @return HTTP/200 on success, HTTP/404 on any error. 
	 */
	@POST
	@Path("/update")
	public Response updatePerson(@Auth Person user, Person p) {
		if (canEditPerson(user, p) == false) { 
			throw new WebApplicationException("You are not permitted to do this", Status.UNAUTHORIZED);
		}
		int numRows = dao.update(p);
		
		if (p.getPositionJson() != null) { 
			//TODO: Maybe update position? 
		}
		
		
		return (numRows == 1) ? Response.ok().build() : Response.status(Status.NOT_FOUND).build();
	}
	
	private boolean canEditPerson(Person editor, Person subject) { 
		if (editor.getId().equals(subject.getId())) { 
			return true;
		}
		Position editorPos = editor.getPositionJson();
		if (editorPos == null) { return false; } 
		if (editorPos.getType() == PositionType.ADMINISTRATOR) { return true; } 
		if (editorPos.getType() == PositionType.SUPER_USER) { 
			//Ensure that the editor is the Super User for the subject's organization.
			Position subjectPos = subject.getPosition();
			if (subjectPos != null && subjectPos.getOrganizationJson() != null &&
					editorPos.getOrganizationJson() != null && 
					subjectPos.getOrganizationJson().getId().equals(editorPos.getOrganizationJson().getId())) { 
				return true;
			}
		}
		return false;
	}

//	@DELETE
//	@Path("/{id}")
//	public Response deletePerson(@PathParam("id") int id) {
//		//TODO: should this operation be allowed?
		//TODO: no, this should soft delete! 
//		dao.deletePersonById(id);
//		return Response.ok().build();
//	}
	
	/**
	 * Searches people in the ANET database TODO: should be fuzzy searching
	 * @param query the search term
	 * @param role either PRINCIPAL, or ADVISOR will search people with that role. 
	 * @return a list of people objects
	 */
	@POST
	@GraphQLFetcher
	@Path("/search")
	public List<Person> search(@GraphQLParam("query") PersonSearchQuery query) {
		return dao.search(query);
	}
	
	@GET
	@Path("/search")
	public List<Person> search(@Context HttpServletRequest request) {
		try { 
			return search(ResponseUtils.convertParamsToBean(request, PersonSearchQuery.class));
		} catch (IllegalArgumentException e) { 
			throw new WebApplicationException(e.getMessage(), e.getCause(), Status.BAD_REQUEST);
		}
	}
	
	/**
	 * Fetches the current position that a given person  is in. 
	 * @param personId the ID number of the person whose position you want to lookup
	 */
	@GET
	@Path("/{id}/position")
	public Position getPositionForPerson(@PathParam("personId") int personId) { 
		return AnetObjectEngine.getInstance().getPositionDao().getCurrentPositionForPerson(Person.createWithId(personId));
	}
	
	@GET
	@GraphQLFetcher
	@Path("/recent")
	public List<Person> recents(@Auth Person user) { 
		return dao.getRecentPeople(user);
	}
	
	@GET
	@GraphQLFetcher("me")
	@Path("/me")
	public Person getCurrentUser(@Auth Person user) { 
		return user;
	}
	
}
