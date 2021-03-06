/*
 * Copyright (c) 2013-2015, Arjuna Technologies Limited, Newcastle-upon-Tyne, England. All rights reserved.
 */

package com.arjuna.databroker.control.ws;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.arjuna.databroker.control.comms.AdvertNodeDTO;
import com.arjuna.databroker.metadata.MetadataContentStore;
import com.arjuna.databroker.metadata.store.AccessControlUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

@Path("/metadata")
@Stateless
public class AdvertsWS
{
    private static final Logger logger = Logger.getLogger(AdvertsWS.class.getName());

    @GET
    @Path("/adverts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AdvertNodeDTO> getAdverts(@QueryParam("requesterid") String requesterId, @QueryParam("userid") String userId)
    {
        logger.log(Level.FINE, "AdvertsWS.listMetadata: [" + requesterId + "][" + userId + "]");
        try
        {
            if ((requesterId == null) && (userId != null))
            {
                logger.log(Level.WARNING, "getAdverts: Invalid parameters: requesterId=[" + requesterId + "], userId=[" + userId + "]");
                return Collections.emptyList();
            }

            List<AdvertNodeDTO> result          = new LinkedList<AdvertNodeDTO>();
            List<String>        metadataBlogIds = _accessControlUtils.listAccessable(requesterId, userId);
 
            for (String metadataBlogId: metadataBlogIds)
            {
                Map<String, AdvertNodeDTO> advertMap = new HashMap<String, AdvertNodeDTO>();

                scanMetadataBlob(metadataBlogId, advertMap);

                result.addAll(advertMap.values());
            }

            return result;
        }
        catch (Throwable throwable)
        {
            logger.log(Level.WARNING, "getAdverts: Unable to metadata", throwable);

            return Collections.emptyList();
        }
    }

    private static final String[] knownRootNodeTypeURIs =
    {
        "http://rdfs.arjuna.com/datasource#DataSource",
        "http://rdfs.arjuna.com/jdbc/postgresql#Database",
        "http://rdfs.arjuna.com/xssf#Spreadsheet"
    };

    private void scanMetadataBlob(String metadataBlogId, Map<String, AdvertNodeDTO> advertMap)
    {
        try
        {
            String content = _metadataContentStore.getContent(metadataBlogId);

            Model  model  = ModelFactory.createDefaultModel();
            Reader reader = new StringReader(content);
            model.read(reader, null);
            reader.close();

            Property rdfTypeProperty = model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "type");

            for (String knownRootNodeTypeURI: knownRootNodeTypeURIs)
            {
                Resource     knownRootNodeTypeResource = model.getResource(knownRootNodeTypeURI);
                StmtIterator statements                = model.listStatements(null, rdfTypeProperty, knownRootNodeTypeResource);

                while (statements.hasNext())
                {
                    Statement statement = statements.nextStatement();
                    Resource  subject   = statement.getSubject();

                    scanSubject(subject, metadataBlogId, true, advertMap);
                }
            }
        }
        catch (Throwable throwable)
        {
            logger.log(Level.WARNING, "Unable to map metadata blob", throwable);
        }
    }

    private static final String[] knownChildPropertyURIs =
    {
        "http://rdfs.arjuna.com/datasource#hasDataService",
        "http://rdfs.arjuna.com/datasource#producesDataSet",
        "http://rdfs.arjuna.com/datasource#hasField",
        "http://rdfs.arjuna.com/access#hasAccessInfo",
        "http://rdfs.arjuna.com/jdbc/postgresql#hasDatabaseTable",
        "http://rdfs.arjuna.com/jdbc/postgresql#hasTableField",
    };

    private AdvertNodeDTO scanSubject(Resource subject, String metadataBlogId, Boolean rootNode, Map<String, AdvertNodeDTO> advertMap)
    {
        if (! advertMap.containsKey(subject.getURI()))
        {
            AdvertNodeDTO advertNode = obtainAdvertNode(subject, metadataBlogId, rootNode, advertMap);

            List<String> childNodeIds = new LinkedList<String>();
            for (String knownChildPropertyURI: knownChildPropertyURIs)
            {
                Property     knownChildProperty = subject.getModel().getProperty(knownChildPropertyURI);
                StmtIterator statements         = subject.getModel().listStatements(subject, knownChildProperty, (RDFNode) null);

                while (statements.hasNext())
                {
                    Statement statement    = statements.nextStatement();
                    Resource  childSubject = statement.getResource();

                    AdvertNodeDTO childAdvertNode = scanSubject(childSubject, metadataBlogId, false, advertMap);

                    childNodeIds.add(childAdvertNode.getId());
                }
            }
            advertNode.setChildNodeIds(childNodeIds);

            return advertNode;
        }
        else
            return obtainAdvertNode(subject, metadataBlogId, rootNode, advertMap);
    }

    private AdvertNodeDTO obtainAdvertNode(Resource subject, String metadataBlogId, Boolean rootNode, Map<String, AdvertNodeDTO> advertMap)
    {
        AdvertNodeDTO result = advertMap.get(subject.getURI());
        if (result == null)
        {
            result = createAdvertNode(subject, metadataBlogId, rootNode);
            advertMap.put(subject.getURI(), result);
        }
        else
            result.setRootNode(rootNode || result.getRootNode());

        return result;
    }

    private AdvertNodeDTO createAdvertNode(Resource subject, String metadataBlogId, Boolean rootNode)
    {
        AdvertNodeDTO advertNode = null;

        try
        {
            logger.log(Level.FINE, "createAdvertNode: subject - " + subject.getURI());

            Property  hasTitle         = subject.getModel().getProperty("http://rdfs.arjuna.com/description#", "hasTitle");
            Property  hasSummary       = subject.getModel().getProperty("http://rdfs.arjuna.com/description#", "hasSummary");
            Property  hasDetails       = subject.getModel().getProperty("http://rdfs.arjuna.com/description#", "hasDetails");
            Statement titleStatement   = subject.getProperty(hasTitle);
            Statement summaryStatement = subject.getProperty(hasSummary);
            Statement detailsStatement = subject.getProperty(hasDetails);

            String       id           = UUID.randomUUID().toString();
            String       metadataId   = metadataBlogId;
            String       metadataPath = subject.getURI();
            String       nodeClass    = null;
            String       name         = null;
            String       summary      = null;
            String       description  = null;
            Date         dataCreated  = null;
            Date         dateUpdate   = null;
            String       owner        = null;
            List<String> tags         = Collections.<String>emptyList();
            List<String> childNodeIds = Collections.<String>emptyList();

            if (titleStatement != null)
                name = titleStatement.getString();
            if (summaryStatement != null)
                summary = summaryStatement.getString();
            if (detailsStatement != null)
                description = detailsStatement.getString();

            advertNode = new AdvertNodeDTO(id, metadataId, metadataPath, rootNode, nodeClass, name, summary, description, dataCreated, dateUpdate, owner, tags, childNodeIds);
        }
        catch (Throwable throwable)
        {
            logger.log(Level.WARNING, "Unable to create advert", throwable);
        }
        
        return advertNode;
    }

    @EJB
    private MetadataContentStore _metadataContentStore;

    @EJB
    private AccessControlUtils _accessControlUtils;
}
