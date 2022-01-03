package cn.lph;

/**
 * 计算类接口
 * Created by xsls on 2022/01/03.   lph
 */
public interface Calculate {

    /**
     * 加法
     * @param numA
     * @param numB
     * @return
     */
     int add(int numA, int numB);

    /**
     * 减法
     * @param numA
     * @param numB
     * @return
     */
     int sub(int numA, int numB);

    /**
     * 除法
     * @param numA
     * @param numB
     * @return
     */
     int div(int numA, int numB);

    /**
     * 乘法
     * @param numA
     * @param numB
     * @return
     */
     int multi(int numA, int numB);

    int mod(int numA, int numB);
}
