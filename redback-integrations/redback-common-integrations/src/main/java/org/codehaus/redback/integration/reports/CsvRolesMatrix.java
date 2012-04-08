package org.codehaus.redback.integration.reports;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.users.UserManager;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.User;
import org.codehaus.redback.integration.util.RoleSorter;
import org.codehaus.redback.integration.util.UserComparator;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CsvRolesMatrix
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
@Service( "report#rolesmatrix-csv" )
public class CsvRolesMatrix
    implements Report
{
    @Inject
    private SecuritySystem securitySystem;

    @Inject
    @Named( value = "rBACManager#jdo" )
    private RBACManager rbacManager;

    public String getName()
    {
        return "Roles Matrix";
    }

    public String getType()
    {
        return "csv";
    }

    public void writeReport( OutputStream os )
        throws ReportException
    {
        UserManager userManager = securitySystem.getUserManager();

        List<User> allUsers = userManager.getUsers();
        List<Role> allRoles;
        Map<String, List<String>> assignmentsMap;

        try
        {
            allRoles = rbacManager.getAllRoles();
            Collections.sort( allRoles, new RoleSorter() );

            assignmentsMap = new HashMap<String, List<String>>();

            for ( UserAssignment assignment : rbacManager.getAllUserAssignments() )
            {
                assignmentsMap.put( assignment.getPrincipal(), assignment.getRoleNames() );
            }
        }
        catch ( RbacManagerException e )
        {
            throw new ReportException( "Unable to obtain list of all roles.", e );
        }

        Collections.sort( allUsers, new UserComparator( "username", true ) );

        PrintWriter out = new PrintWriter( os );

        writeCsvHeader( out, allRoles );

        for ( User user : allUsers )
        {
            writeCsvRow( out, user, assignmentsMap, allRoles );
        }

        out.flush();
    }

    private void writeCsvHeader( PrintWriter out, List<Role> allRoles )
    {
        out.print( "Username,Full Name,Email Address" );
        for ( Role role : allRoles )
        {
            out.print( "," + escapeCell( role.getName() ) );
        }
        out.println();
    }

    private void writeCsvRow( PrintWriter out, User user, Map<String, List<String>> assignmentsMap,
                              List<Role> allRoles )
    {
        out.print( escapeCell( user.getUsername() ) );
        out.print( "," + escapeCell( user.getFullName() ) );
        out.print( "," + escapeCell( user.getEmail() ) );

        List<String> assignedRoleNames = assignmentsMap.get( user.getPrincipal().toString() );
        if ( assignedRoleNames == null )
        {
            assignedRoleNames = new ArrayList<String>();
        }

        for ( Role role : allRoles )
        {
            out.print( ',' );
            if ( assignedRoleNames.contains( role.getName() ) )
            {
                out.print( 'Y' );
            }
        }
        out.println();
    }

    private String escapeCell( String cell )
    {
        return "\"" + StringEscapeUtils.escapeJava( cell ) + "\"";
    }

    public String getId()
    {
        return "rolesmatrix";
    }

    public String getMimeType()
    {
        return "text/csv";
    }
}