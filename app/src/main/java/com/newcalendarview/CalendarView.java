package com.newcalendarview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by gan on 2016/7/19.
 */
public class CalendarView extends View implements View.OnTouchListener {
    public static final long ONE_DAY = 1000 * 60 * 60 * 24;
    public static final long ONE_MONTH = ONE_DAY * 30;

    private Surface surface;
    private Calendar calendar;
    private int[][] monthDay = new int[6][7]; //储存该月所有日期的数组
    private Map<String, Integer> dataMap = new HashMap<>();

    private Date today;
    private Date curDate; //当前的日期
    private Date chosenDate; //选择到的日期
    private long chosenDateStamp; //选择到的日期的时间戳
    private int lastMonthDay = 0; //之前需要补几天
    private int nextMonthDay = 0; //之后需要加几天，如果它除以7为0，说明6行；除以7为1，有5行；除以7为2，有4行
    private int row; //日历的行数
    private OnItemClickListener onItemClickListener;
    private float x1;
    private float x2;

    private UpdateListener updateListener;

    public CalendarView(Context context) {
        this(context, null);
    }

    public CalendarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        calendar = Calendar.getInstance();

        //初始化背景和画图的工具类
        surface = new Surface();
        surface.density = getResources().getDisplayMetrics().density;
        setBackgroundColor(surface.bgColor);

        //当前的日期
        curDate = new Date();
        //今天的日期
        today = new Date();

        chosenDate = curDate;
        chosenDateStamp = chosenDate.getTime();


        //第一次加载时，选择的日期是当前日期
        calculateDate(today);
        setOnTouchListener(this);

    }

    /**
     * 根据选中的日期来得到选中日期月份的数组
     *
     * @param chosenDate 选中的日期
     */
    private void calculateDate(Date chosenDate) {
        nextMonthDay = 0;
        calendar.setTime(chosenDate);

        //获得当月的天数
        calendar.set(Calendar.DATE, 1);
        calendar.roll(Calendar.DATE, -1);
        int maxDate = calendar.get(Calendar.DATE);


        //获得第一天是周几
        calendar.set(Calendar.DAY_OF_MONTH, 1); //获得本月第一天
        lastMonthDay = calendar.get(Calendar.DAY_OF_WEEK) - 1;


        int t = 1;
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                monthDay[i][j] = (t - lastMonthDay);
                t++;
                if (monthDay[i][j] > maxDate) {
                    monthDay[i][j] = 0;
                    nextMonthDay++;
                }
            }
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        surface.width = getResources().getDisplayMetrics().widthPixels;
        switch (nextMonthDay / 7) {
            case 0:
                surface.height = (int) (surface.weekHeight + surface.cellHeight * 6);
                row = 6;
                break;
            case 1:
                surface.height = (int) (surface.weekHeight + surface.cellHeight * 5);
                row = 5;
                break;
            case 2:
                surface.height = (int) (surface.weekHeight + surface.cellHeight * 4);
                row = 4;
                break;
        }
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(surface.width,
                MeasureSpec.EXACTLY);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(surface.height,
                MeasureSpec.EXACTLY);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            surface.init();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Log.d("TAG", "onDraw");

        // 画出星期和格子
        drawWeekAndBoard(canvas, surface.weekBgColor);

        int[] todayXAndY = getDataXAndY(today);

        //先画出所有日期
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 7; j++) {
                if (monthDay[i][j] > 0) {
                    //如果当前的日期和今天的日期相同，并且他们在同一个月中，那么当前日期就是今天
                    if (j == todayXAndY[0] && i == todayXAndY[1] && DateFormatUtil.date2String(today, "MM")
                            .contains(String.valueOf(getMonth())) && String.valueOf(monthDay[i][j])
                            .equals(DateFormatUtil.date2String(today, "dd"))) {
                        drawTodayText(canvas, i, j, monthDay[i][j] + "", surface.textColor);
                    } else {
                        drawCellText(canvas, i, j, monthDay[i][j] + "", surface.textColor);
                    }
                }
            }
        }

        //再画出当前选中的日期(默认为当天),并且只有选中的日期处在当前月份和年份才显示
        if (DateFormatUtil.date2String(chosenDate, "MM").contains(String.valueOf(getMonth()))
                && DateFormatUtil.date2String(chosenDate, "yyyy").contains(String.valueOf(getYear()))) {
            int[] xAndy = getDataXAndY(chosenDate);
            drawCellBg(canvas, xAndy[1], xAndy[0], surface.cellSelectedColor);
            if (chosenDate.getTime() == today.getTime()) {
                drawTodayText(canvas, xAndy[1], xAndy[0], monthDay[xAndy[1]][xAndy[0]] + "", surface
                        .cellTextSelectedColor);
            } else {
                drawCellText(canvas, xAndy[1], xAndy[0], monthDay[xAndy[1]][xAndy[0]] + "", surface
                        .cellTextSelectedColor);
            }
        }
    }


    /**
     * 画星期和分割线
     */
    private void drawWeekAndBoard(Canvas canvas, int color) {
        canvas.drawPath(surface.boxPath, surface.borderPaint);
        float weekTextY = surface.weekHeight * 3 / 4f;
        canvas.drawRect(0, 0, surface.width, surface.weekHeight, surface.weekBgPaint);
        surface.cellBgPaint.setColor(surface.textColor);
        for (int i = 0; i < surface.weekText.length; i++) {
            float weekTextX = i
                    * surface.cellWidth
                    + (surface.cellWidth - surface.weekPaint
                    .measureText(surface.weekText[i])) / 2f;
            canvas.drawText(surface.weekText[i], weekTextX, weekTextY,
                    surface.weekPaint);
        }
    }

    /**
     * 画出具体日期的数字
     */
    private void drawCellText(Canvas canvas, int y, int x, String text, int color) {
        surface.datePaint.setColor(color);
        float cellY = surface.weekHeight + y
                * surface.cellHeight + surface.cellHeight * 3 / 5f;
        float cellX = (surface.cellWidth * x)
                + (surface.cellWidth - surface.datePaint.measureText(text))
                / 2f;
        canvas.drawText(text, cellX, cellY, surface.datePaint);
    }


    /***
     * 画出今日的text
     */
    private void drawTodayText(Canvas canvas, int y, int x, String text, int color) {

        surface.datePaint.setColor(color);
        float cellY = surface.weekHeight + y
                * surface.cellHeight + surface.cellHeight * 3 / 7f;
        float cellTextY = surface.weekHeight + y
                * surface.cellHeight + surface.cellHeight * 5 / 7f;
        float cellX = (surface.cellWidth * x
                + (surface.cellWidth - surface.datePaint.measureText(text))
                / 2f);
        float cellTextX = (surface.cellWidth * x)
                + (surface.cellWidth - surface.datePaint.measureText(text))
                * 1 / 4f;
        canvas.drawText(text, cellX, cellY, surface.datePaint);
        canvas.drawText("(今日)", cellTextX, cellTextY, surface.datePaint);
    }

    /**
     * 画每一个格子的背景
     */
    private void drawCellBg(Canvas canvas, int y, int x, int color) {

        surface.cellBgPaint.setColor(color);
        float left = surface.cellWidth * x + surface.borderWidth;
        float top = surface.weekHeight + y
                * surface.cellHeight + surface.borderWidth;
        canvas.drawRect(left, top, left + surface.cellWidth
                - surface.borderWidth, top + surface.cellHeight
                - surface.borderWidth, surface.cellBgPaint);
    }


    /**
     * 获得当前日期在数组中的x和y
     */

    private int[] getDataXAndY(Date date) {
        int[] xAndY = new int[2];
        calendar.setTime(date);
        //date在这个月的第几天
        int num = calendar.get(Calendar.DAY_OF_MONTH);

        //获得第一天是周几
        calendar.set(Calendar.DAY_OF_MONTH, 1); //获得本月第一天
        Date first = calendar.getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("E");
        String week = formatter.format(first);

        int y = num > 7 - lastMonthDay ? (num - 1 - (7 - lastMonthDay)) / 7 + 1 : 0;
        int x = num > 7 - lastMonthDay ? (num - 1 - (7 - lastMonthDay)) % 7 : num + lastMonthDay - 1;
        xAndY[0] = x;
        xAndY[1] = y;
        return xAndY;
    }


    /***
     * 切换到上一个月
     */
    public void clickLastMonth() {
        calendar.setTime(curDate);
        calendar.add(Calendar.MONTH, -1);
        curDate = calendar.getTime();
        calculateDate(curDate);
        requestLayout();
        invalidate();

    }

    /**
     * 切换到下一个月
     */
    public void clickNextMonth() {
        calendar.setTime(curDate);
        calendar.add(Calendar.MONTH, 1);
        curDate = calendar.getTime();
        calculateDate(curDate);
        requestLayout();
        invalidate();
    }


    public void clickNextDay() {
        chosenDateStamp = chosenDate.getTime(); //获得当前选择时间的时间戳
        chosenDateStamp += ONE_DAY;   //给时间戳加一天
        chosenDate = new Date(chosenDateStamp); //获得选择日期
        curDate = chosenDate;   //将当前的日期设置为选择的日期
        calendar.setTime(curDate);
        calculateDate(curDate);
        requestLayout();
        invalidate();
    }

    public void clickLastDay() {
        chosenDateStamp = chosenDate.getTime();
        chosenDateStamp -= ONE_DAY;
        chosenDate = new Date(chosenDateStamp);
        curDate = chosenDate;
        calendar.setTime(curDate);
        calculateDate(curDate);
        requestLayout();
        invalidate();
    }

    /**
     * 回到今日
     */
    public void clickReturnToday() {
        chosenDate = today;
        curDate = today;
        calendar.setTime(curDate);
        calculateDate(curDate);
        onItemClickListener.OnItemClick(chosenDate);

        requestLayout();
        invalidate();
    }

    /**
     * 得到当前日期
     */
    public Date getDate() {
        return chosenDate;
    }

    /**
     * 得到年份
     */
    public int getYear() {
        calendar.setTime(curDate);
        int year = calendar.get(Calendar.YEAR);
        return year;
    }

    /**
     * 得到月份
     */
    public int getMonth() {
        calendar.setTime(curDate);
        int month = calendar.get(Calendar.MONTH) + 1;
        return month;
    }

    /**
     * 通过坐标拿到点击的date
     */
    private Date getClickData(float x, float y) {
        if (y > surface.weekHeight) {
            int m = (int) (Math.floor(x / surface.cellWidth));
            int n = (int) (Math
                    .floor((y - surface.weekHeight)
                            / Float.valueOf(surface.cellHeight)));
            int[] xAndY = new int[2];
            xAndY[0] = m;
            xAndY[1] = n;

            calendar.setTime(curDate);
            calendar.set(Calendar.DAY_OF_MONTH, 1); //获得本月第一天
            if ((n == 0 && m <= lastMonthDay - 1) || (n == row - 1 && m > (7 - nextMonthDay % 7 - 1))) {
                return chosenDate;
            }
            //根据点击的坐标，计算得到点击的dat
            if (n > 0) {
                calendar.add(Calendar.DATE, (7 - lastMonthDay) + (n - 1) * 7 + m);
            } else {
                calendar.add(Calendar.DATE, m - lastMonthDay);
            }
            return calendar.getTime();
        }
        return chosenDate;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                if (x2 - x1 > 50) {
                    clickNextMonth();
                    updateListener.update();
                } else if (x1 - x2 > 50) {
                    clickLastMonth();
                    updateListener.update();
                } else {
                    chosenDate = getClickData(event.getX(), event.getY());
                    invalidate();
                    onItemClickListener.OnItemClick(chosenDate);
                }
                break;
        }
        return true;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    //监听接口
    public interface OnItemClickListener {
        void OnItemClick(Date date);
    }

    private class Surface {
        public float density;
        public int width; // 整个控件的宽度
        public int height; // 整个控件的高度


        public float weekHeight; // 显示星期的高度
        public float cellWidth; // 日期方框宽度
        public float cellHeight; // 日期方框高度
        public float borderWidth; //边框宽度
        public int bgColor = Color.parseColor("#1E2124"); //背景颜色

        private int textColor = Color.parseColor("#DCDCDC"); //文字颜色

        private int weekColor = Color.parseColor("#282B2E"); //星期颜色


        private int btnColor = Color.parseColor("#666666");

        private int borderColor = Color.parseColor("#333333"); //边界线颜色

        private int weekBgColor = Color.parseColor("#282B2E"); //星期背景的颜色


        public int cellDownColor = Color.parseColor("#FAC814"); //按下的颜色

        public int cellSelectedColor = Color.parseColor("#FAC814"); //选中的颜色

        public int cellTextSelectedColor = Color.parseColor("#282B2E"); //文字选中的颜色

        public Paint borderPaint;
        public Paint monthPaint;
        public Paint weekPaint;
        public Paint weekBgPaint;
        public Paint datePaint;
        public Paint monthChangeBtnPaint;
        public Paint cellBgPaint;
        public Path boxPath; // 边框路径


        public String[] weekText = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};

        public void init() {
            float temp = height / 7f;


            //星期的高度
            weekHeight = 58f;

            //设置日期边界大小
            cellWidth = (width - 6 * borderWidth) / 7;
            cellHeight = cellWidth;


/**
 *****************************设置分割线的属性*************************
 */

            borderPaint = new Paint();
            borderPaint.setColor(borderColor);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderWidth = 1f;
            borderPaint.setStrokeWidth(borderWidth);

/**
 *****************************设置星期的属性*************************
 */

            weekPaint = new Paint();
            weekPaint.setColor(textColor);
            weekPaint.setAntiAlias(true);
            float weekTextSize = DateFormatUtil.sp2px(getContext(), 13f);
            weekPaint.setTextSize(weekTextSize);
            weekBgPaint = new Paint();
            weekBgPaint.setColor(weekBgColor);
            weekBgPaint.setAntiAlias(true);
            weekBgPaint.setStyle(Paint.Style.FILL);
            weekBgPaint.setColor(weekBgColor);

/**
 *****************************设置日期的属性*************************
 */

            datePaint = new Paint();
            datePaint.setColor(textColor);
            datePaint.setAntiAlias(true);
            float cellTextSize = DateFormatUtil.sp2px(getContext(), 15f);
            datePaint.setTextSize(cellTextSize);

/**
 *****************************分割线的路径*************************
 */

            boxPath = new Path();

            boxPath.moveTo(0, weekHeight);
            boxPath.rLineTo(width, 0);
            boxPath.moveTo(0, weekHeight);


            for (int i = 1; i < 6; i++) {
                boxPath.moveTo(0, weekHeight + i * cellHeight);
                boxPath.rLineTo(width, 0);
                boxPath.moveTo(i * cellWidth, weekHeight);
                boxPath.rLineTo(0, height);
            }

            boxPath.moveTo(6 * cellWidth, weekHeight);
            boxPath.rLineTo(0, height);

            monthChangeBtnPaint = new Paint();
            monthChangeBtnPaint.setAntiAlias(true);
            monthChangeBtnPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            monthChangeBtnPaint.setColor(btnColor);

            cellBgPaint = new Paint();
            cellBgPaint.setAntiAlias(true);
            cellBgPaint.setStyle(Paint.Style.FILL);
            cellBgPaint.setColor(cellSelectedColor);
        }
    }

    public void setUpdateListener(UpdateListener updateListener) {
        this.updateListener = updateListener;
    }

    public interface UpdateListener {
        void update();
    }


}
