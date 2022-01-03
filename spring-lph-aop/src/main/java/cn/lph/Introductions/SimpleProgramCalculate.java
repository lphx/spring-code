package cn.lph.Introductions;

/***
 * Created by xsls on 2022/01/03.   lph
 */
public class SimpleProgramCalculate implements ProgramCalculate{
    @Override
    public String toBinary(Integer value) {
        return Integer.toBinaryString(value);
    }
}
