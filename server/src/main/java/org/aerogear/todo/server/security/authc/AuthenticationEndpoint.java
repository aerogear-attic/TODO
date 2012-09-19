/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.aerogear.todo.server.security.authc;

import org.jboss.logging.Logger;
import org.jboss.picketlink.cdi.Identity;
import org.jboss.picketlink.cdi.credential.Credential;
import org.jboss.picketlink.cdi.credential.LoginCredentials;
import org.jboss.picketlink.idm.IdentityManager;
import org.jboss.picketlink.idm.model.Group;
import org.jboss.picketlink.idm.model.Role;
import org.jboss.picketlink.idm.model.User;
import org.picketbox.cdi.PicketBoxUser;
import org.picketbox.core.authentication.credential.UsernamePasswordCredential;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * <p>JAX-RS Endpoint to authenticate users.</p>
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
@Stateless
@Path("/auth")
@TransactionAttribute
public class AuthenticationEndpoint {

    @Inject
    private Identity identity;

    @Inject
    private LoginCredentials credential;

    @Inject
    private IdentityManager identityManager;

    private static final Logger LOGGER = Logger.getLogger(AuthenticationEndpoint.class);

    /**
     * <p>Loads some users during the first construction.</p>
     */
    @PostConstruct
    public void loadUsers() {
        User john = this.identityManager.createUser("john");

        john.setEmail("john@doe.org");
        john.setFirstName("John");
        john.setLastName("Doe");

        this.identityManager.updatePassword(john, "123");

        Role roleDeveloper = this.identityManager.createRole("developer");
        Role roleAdmin = this.identityManager.createRole("admin");

        Group groupCoreDeveloper = identityManager.createGroup("Core Developers");

        identityManager.grantRole(roleDeveloper, john, groupCoreDeveloper);
        identityManager.grantRole(roleAdmin, john, groupCoreDeveloper);

    }

    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationResponse register(final AuthenticationRequest authcRequest) {

        LOGGER.debug("My pretty registered user: " + authcRequest.getFirstName());
        return null;
    }

    /**
     * <p>Performs the authentication using the informations provided by the {@link AuthenticationRequest}</p>
     *
     * @param authcRequest
     * @return
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationResponse login(final AuthenticationRequest authcRequest) {

        LOGGER.debug("Logged in!");

        if (this.identity.isLoggedIn()) {
            return createResponse(authcRequest);
        }

        credential.setUserId(authcRequest.getUserId());
        credential.setCredential(new Credential<UsernamePasswordCredential>() {

            @Override
            public UsernamePasswordCredential getValue() {
                return new UsernamePasswordCredential(authcRequest.getUserId(), authcRequest.getPassword());
            }
        });

        this.identity.login();

        return createResponse(authcRequest);
    }

    @POST
    @Path("/logout")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void logout(final AuthenticationRequest authcRequest) {
        LOGGER.debug("See ya!");
        if (this.identity.isLoggedIn()) {
            this.identity.logout();
        }
    }

    private AuthenticationResponse createResponse(AuthenticationRequest authcRequest) {
        AuthenticationResponse response = new AuthenticationResponse();

        response.setUserId(authcRequest.getUserId());
        response.setLoggedIn(this.identity.isLoggedIn());

        if (response.isLoggedIn()) {
            PicketBoxUser user = (PicketBoxUser) this.identity.getUser();

            response.setToken(user.getSubject().getSession().getId().getId().toString());
        }

        return response;
    }

}