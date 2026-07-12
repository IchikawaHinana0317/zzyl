package com.zzyl.nursing.vo.health;

import lombok.Data;

@Data
public class SystemScore {

    /**
     * 呼吸系统
     */
    private Integer breathingSystem;

    /**
     * 消化系统
     */
    private Integer digestiveSystem;

    /**
     * 内分泌系统
     */
    private Integer endocrineSystem;

    /**
     * 免疫系统
     */
    private Integer immuneSystem;

    /**
     * 循环系统
     */
    private Integer circulatorySystem;

    /**
     * 泌尿系统
     */
    private Integer urinarySystem;

    /**
     * 感觉系统
     */
    private Integer motionSystem;

    /**
     * 感官系统
     */
    private Integer senseSystem;
}