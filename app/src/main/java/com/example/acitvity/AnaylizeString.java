package com.example.acitvity;

/*
    解析数据
 */
public class AnaylizeString {

    private int flag;
    private String input;
    public double  NO2,CO,CO2,O3,SO2,tep,wet,PM25,Total;
    public double MAXC;
    public double AQI_Final,AQI_NO2,AQI_CO,AQI_SO2,AQI_O3,AQI_PM25;
    final static int []AQI={0,50,100,150,200,300,400,500};
    final double []CNO2={0,100,200,700,1200,2340,3090,3840};
    final double []CSO2={0,150,500,650,800,1600,2100,2620};
    final double []CCO={0,5,10,35,60,90,120,150};
    final double []CO3={0,100,160,215,265,800,1000,1200};
    final double []CPM25={0,35,75,115,150,250,350,500};

    AnaylizeString()
    {
        NO2=CO=CO2=O3=SO2=tep=wet=PM25=Total=0;
    }
    //计算api的值
    private double PubMot(double ch,double cl,double Ih,double Il,double C)
    {
        return (Ih-Il)/(ch-cl)*(C-cl)+Il;
    }

    //解析AQI算法
    private double AnalizeAQI(double []C,double Cs)
    {
        double tempAQI=0;
        if(C[0]<Cs&&Cs<=C[1])
        {
            tempAQI=PubMot(C[1],C[0],AQI[1],AQI[0],Cs);
        }
        else
        {
            for(int i=1;i<7;i++)
            {
                if(C[i]<Cs&&Cs<=C[i+1])
                {
                    tempAQI=PubMot(C[i+1],C[i]+1,AQI[i+1],AQI[i]+1,Cs);
                    break;
                }
            }
        }
        return tempAQI;
    }

    //解析数据
    public void Analize(String s)
    {
        Analize_String(s);
        double MaxAqi=0;
        if(MaxAqi<AnalizeAQI(CCO,CO))
            MaxAqi=AnalizeAQI(CCO,CO);
        if(MaxAqi<AnalizeAQI(CNO2,NO2))
            MaxAqi=AnalizeAQI(CNO2,NO2);
        if(MaxAqi<AnalizeAQI(CO3,O3))
            MaxAqi=AnalizeAQI(CO3,O3);
        if(MaxAqi<AnalizeAQI(CSO2,SO2))
            MaxAqi=AnalizeAQI(CSO2,SO2);
        if(MaxAqi<AnalizeAQI(CPM25,PM25))
            MaxAqi=AnalizeAQI(CPM25,PM25);
        AQI_Final=MaxAqi;



    }
    //字符串
    private void Analize_String(String s)
    {
        String temp="";
        flag=0;
        int flagType=0;
        MAXC=0;
        for(int i=0;i<s.length()&&s.charAt(i)!='E';i++)
        {
            if(flag==0)
            {
                if(s.charAt(i)=='S')
                {
                    flag=1;
                    i+=5;
                }
            }
            else
            {
                for(;i<s.length()&&s.charAt(i)!=' ';i++)
                {
                    temp+=s.charAt(i);
                }
                flagType++;
                switch (flagType)
                {
                    case 1:
                        NO2=Double.parseDouble(temp);
                        if(MAXC<NO2)
                            MAXC=NO2;
                        break;
                    case 2:
                        CO=Double.parseDouble(temp);
                        if(MAXC<CO)
                            MAXC=CO;
                        break;
                    case 3:
                        CO2=Double.parseDouble(temp);
                        if(MAXC<CO2)
                            MAXC=CO2;
                        break;
                    case 4:
                        O3=Double.parseDouble(temp);
                        if(MAXC<O3)
                            MAXC=O3;
                        break;
                    case 5:
                        SO2=Double.parseDouble(temp);
                        if(MAXC<SO2)
                            MAXC=SO2;
                        break;
                    case 6:
                        tep=Double.parseDouble(temp);
                        if(MAXC<tep)
                            MAXC=tep;
                        break;
                    case 7:
                        wet=Double.parseDouble(temp);
                        if(MAXC<wet)
                            MAXC=wet;
                        break;
                    case 8:
                        PM25=Double.parseDouble(temp);
                        if(MAXC<PM25)
                            MAXC=PM25;
                        break;
                    case 9:
                        Total=Double.parseDouble(temp);
                        break;
                    default:
                        break;
                }
                temp="";
            }
        }
    }
   
}
