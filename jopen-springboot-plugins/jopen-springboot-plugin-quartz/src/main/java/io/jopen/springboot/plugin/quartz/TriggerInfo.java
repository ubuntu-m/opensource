package io.jopen.springboot.plugin.quartz;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author maxuefeng
 * @since 2020/1/31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@lombok.Builder(builderClassName = "Builder", toBuilder = true)
@Getter
@Setter
public class TriggerInfo {
    private String group;

    private String name;

    private String cron;

    private String state;

    private Date endTime;

    private Date previousFireTime;

    private Date nextFireTime;
}
