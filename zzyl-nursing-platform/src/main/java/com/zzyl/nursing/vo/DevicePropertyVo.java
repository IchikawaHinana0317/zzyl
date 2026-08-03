package com.zzyl.nursing.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("设备上报属性")
public class DevicePropertyVo {

    @ApiModelProperty("属性标识")
    private String functionId;

    @ApiModelProperty("属性值")
    private Object value;

    @ApiModelProperty("上报时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventTime;
}