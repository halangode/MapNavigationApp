
package com.halangode.mapsnavigationapp.webservice.bean;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ViaWaypoint {

    @SerializedName("location")
    @Expose
    private Location location;
    @SerializedName("step_index")
    @Expose
    private Integer stepIndex;
    @SerializedName("step_interpolation")
    @Expose
    private Double stepInterpolation;

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Integer getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(Integer stepIndex) {
        this.stepIndex = stepIndex;
    }

    public Double getStepInterpolation() {
        return stepInterpolation;
    }

    public void setStepInterpolation(Double stepInterpolation) {
        this.stepInterpolation = stepInterpolation;
    }

}
