/*
 * Copyright (c) 2013-2014, Arjuna Technologies Limited, Newcastle-upon-Tyne, England. All rights reserved.
 */

package com.arjuna.databroker.webportal;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@SessionScoped
@ManagedBean(name="navigation")
public class NavigationMO implements Serializable
{
    private static final long serialVersionUID = 4384414923929789797L;

    public NavigationMO()
    {
        _locations = new LinkedList<LocationVO>();

        _locations.add(new LocationVO("Home", "/index?faces-redirect=true", "fa fa-home"));
        _locations.add(new LocationVO("Search", "/dataviews/dataadverts?faces-redirect=true", "fa fa-search"));
        _locations.add(new LocationVO("Data Flows", "/dataflows/dataflows?faces-redirect=true", "fa fa-puzzle-piece"));
        _locations.add(new LocationVO("Config", "/users/users?faces-redirect=true", "fa fa-cog"));
    }

    public List<LocationVO> getLocations()
    {
        return _locations;
    }

    public String doGoTo(String name)
    {
        String page = null;

        ListIterator<LocationVO> locationIterator = _locations.listIterator();

        while (locationIterator.hasNext())
        {
            LocationVO currentLocation = locationIterator.next();

            if ((page == null) && name.equals(currentLocation.getName()))
                page = currentLocation.getPage();

//            if (page != null)
//                locationIterator.remove();
//            else if (name.equals(currentLocation.getName()))
//                page = currentLocation.getPage();
        }
        
        if (page != null)
            return page;
        else
            return "#";
    }

    private List<LocationVO> _locations;
}
