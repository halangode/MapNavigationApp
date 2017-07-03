package com.halangode.mapsnavigationapp.util;

/**
 * Created by Harikumar Alangode on 01-Jul-17.
 */

import com.google.android.gms.maps.model.LatLng;
import com.halangode.mapsnavigationapp.webservice.bean.GoogleDirectionsResponse;
import com.halangode.mapsnavigationapp.webservice.bean.Leg;
import com.halangode.mapsnavigationapp.webservice.bean.Route;
import com.halangode.mapsnavigationapp.webservice.bean.Step;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataParser {

    private static List<List<Step>> allStepList;

    /** Receives a JSONObject and returns a list of lists containing latitude and longitude */
    public List<List<HashMap<String,String>>> parse(GoogleDirectionsResponse response){

        List<List<HashMap<String, String>>> routes = new ArrayList<>() ;
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;

        List<Route> routes1;
        List<Leg> legs;
        List<Step> steps;

        allStepList = new ArrayList<>();

        try {
            routes1 = response.getRoutes();


            //jRoutes = jObject.getJSONArray("routes");
            //jStepsArray = new JSONArray[jRoutes.length()];

            /** Traversing all routes */
            for(int i=0;i<routes1.size();i++){
                //jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");
                legs = routes1.get(i).getLegs();
                List path = new ArrayList<>();

                /** Traversing all legs */
                for(int j=0;j<legs.size();j++){
                    //jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");
                    steps = legs.get(j).getSteps();
                    allStepList.add(steps);
                    //jStepsArray[i] = jSteps;
                    /** Traversing all steps */
                    for(int k=0;k<steps.size();k++){
                        String polyline = "";
                        //polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                        polyline = steps.get(k).getPolyline().getPoints();
                        List<LatLng> list = decodePoly(polyline);

                        /** Traversing all points */
                        for(int l=0;l<list.size();l++){
                            HashMap<String, String> hm = new HashMap<>();
                            hm.put("lat", Double.toString((list.get(l)).latitude) );
                            hm.put("lng", Double.toString((list.get(l)).longitude) );
                            path.add(hm);
                        }
                    }
                    routes.add(path);
                }
            }

        } catch (Exception e){

        }


        return routes;
    }


    /**
     * Method to decode polyline points
     * Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     * */
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }


    public static List<String> getOrderedDirections(int i){
        List<String> directions = new ArrayList<>();
        List<Step> stepList = allStepList.get(i);
        for(int j = 0; j < stepList.size(); j++){
            directions.add(stepList.get(j).getHtmlInstructions());
        }
        return directions;
    }
}
