package io.jopen.springboot.plugin.common.tuple;

import lombok.*;

/**
 * @author maxuefeng
 * @since 2019/8/31
 */
@AllArgsConstructor
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Tuple4<F1, F2, F3, F4> {

    private F1 f1;

    private F2 f2;

    private F3 f3;
    
    private F3 f4;
}
