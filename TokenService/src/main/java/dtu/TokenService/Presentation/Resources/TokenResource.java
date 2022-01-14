package dtu.TokenService.Presentation.Resources;

import dtu.TokenService.Application.TokenService;
import dtu.TokenService.Domain.Repositories.LocalTokenRepository;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/tokens")
public class TokenResource  {

	private static TokenService service = new TokenService(new LocalTokenRepository());

	@POST
	@Path("{customerId}/{numberOfTokens}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(@PathParam("customerId") String customerId, @PathParam("numberOfTokens") Integer numberOfTokens) {
		return Response.status(Response.Status.CREATED).entity(service.createTokens(numberOfTokens,customerId)).build();
	}

	@GET
	@Path("{customerId}")
	public Response get(@PathParam("customerId") String customerId) {
		return Response.status(Response.Status.OK).entity(service.getTokens(customerId)).build();
	}

	@DELETE
	@Path("{customerId}")
	public Response delete(@PathParam("customerId") String customerId) {
		return Response.status(Response.Status.OK).entity(service.deleteTokensForCustomer(customerId)).build();
	}

	//    @GET
	//    public Response get(@QueryParam("id") String id) {
	//        System.out.println(id);
	//        try {
	//            System.out.println(service.getAccount(id));
	//            return Response.status(Response.Status.OK).entity(service.getAccount(id)).build();
	//        } catch (ArgumentNullException e) {
	//            return Response.status(Response.Status.OK).entity(service.getAllAccounts()).build();
	//        } catch (EntityNotFoundException e) {
	//            System.out.println(id);
	//            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
	//        } catch (BankServiceException_Exception e) {
	//            e.printStackTrace();
	//            return null;
	//        }
	//    }
	//
	//
	//    @DELETE
	//    public Response delete(@QueryParam("id")String id) {
	//        return Response.status(Response.Status.OK).entity(service.deleteAccount(id)).build();
	//    }
	//
	//    @POST
	//    @Produces(MediaType.APPLICATION_JSON)
	//    @Consumes(MediaType.APPLICATION_JSON)
	//    public Response create(Account entity) {
	//        return Response.status(Response.Status.CREATED).entity(service.createAccount(entity)).build();
	//    }
}
