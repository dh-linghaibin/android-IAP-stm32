package com.jt28.a6735.android_iap;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.jt28.a6735.ymodem.YModem;
import com.jt28.a6735.ymodem.YModemListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private String DEFAULT_FILENAME = "m_uip.hex";//"miniblink.hex";
    private Button read_hex;

    private YModem ymodem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        read_hex = (Button) findViewById(R.id.id_read_hex);
        read_hex.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    File file = new File(Environment.getExternalStorageDirectory(),
                            DEFAULT_FILENAME);
                    Log.d("lhb","解析" + Environment.getExternalStorageDirectory().toString() + DEFAULT_FILENAME);
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String readline = "";
                    StringBuffer sb = new StringBuffer();

                    int besac_addr = 0;//写入基础地址

                    while ((readline = br.readLine()) != null) {
                        System.out.println("readline:" + readline);
                        sb.append(readline);

//                        Log.d("lhb","转换"+ Arrays.toString(readline.toCharArray()) + "长度" + readline.length());

                        char[] read_c_l =  readline.toCharArray();
                        byte[] read_byte = new byte[read_c_l.length/2];
                        int read_byte_count = 0;
                        for(int i = 1;i < read_c_l.length;i+=2) {
                            byte temp  = charToByte(read_c_l[i+1]);
                            temp += (byte) (charToByte(read_c_l[i]) << 4);
                            read_byte[read_byte_count] = temp;
                            read_byte_count++;
                        }

                        Log.d("lhb","解析" + Arrays.toString(read_byte) + "长度"+read_byte.length + read_c_l[0]);

                        int check = 0;
                        for(int i= 0;i < read_byte.length-1;i++){
                            check += read_byte[i];
                        }
                        byte check_byte = (byte) (0x100 - (0x0000ff&check));
                        if(check_byte == read_byte[read_byte.length-1]) {
                            Log.d("lhb","校验通过");
                        } else {
                            Log.d("lhb","校验失败");
                        }

                        switch(read_byte[3]) {
                            case 0x00:{//'00' Data Rrecord：用来记录数据，HEX文件的大部分记录都是数据记录
                                int offset = read_byte[2];
                                offset += (read_byte[1] << 8);
                                besac_addr += offset;
                                Log.d("lhb","记录数据-写入地址" + besac_addr);
                                break;
                            }
                            case 0x01:{//'01' End of File Record:用来标识文件结束，放在文件的最后，标识HEX文件的结尾
                                Log.d("lhb","文件结束");
                                break;
                            }
                            case 0x02:{//'02' Extended Segment Address Record:用来标识扩展段地址的记录
                                Log.d("lhb","扩展段地址");
                                break;
                            }
                            case 0x03:{//'03' Start Segment Address Record:开始段地址记录
                                Log.d("lhb","开始段地址记录");
                                break;
                            }
                            case 0x04:{//'04' Extended Linear Address Record:用来标识扩展线性地址的记录
                                int offset = read_byte[2];
                                offset += (read_byte[1] << 8);

                                besac_addr = read_byte[5];
                                besac_addr += ((read_byte[4] << 8) << 16 ) + offset;
                                Log.d("lhb","扩展线性地址的记录" + besac_addr);
                                break;
                            }
                            case 0x05:{//'05' Start Linear Address Record:开始线性地址记录
                                Log.d("lhb","开始线性地址记录");
                                break;
                            }
                        }
                    }
                    br.close();
                    System.out.println("读取成功：" + sb.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    /**
     * 判断SDCard是否存在 [当没有外挂SD卡时，内置ROM也被识别为存在sd卡]
     *
     * @return
     */
    public static boolean isSdCardExist() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    private void startTransmission() {

        ymodem = new YModem.Builder()
                .with(this)
                .filePath("file:///sdcard/main.bin")
                .fileName("main.bin")
                .checkMd5("123")
                .callback(new YModemListener() {
                    @Override
                    public void onDataReady(byte[] data) {
                        //send this data[] to your ble component here...
                        Log.d("lhb","接收到" + Arrays.toString(data));
                    }

                    @Override
                    public void onProgress(int currentSent, int total) {
                        //the progress of the file data has transmitted
                        Log.d("lhb","接收到2" + currentSent);
                    }

                    @Override
                    public void onSuccess() {
                        //we are well done with md5 checked
                        Log.d("lhb","接收到4");
                    }

                    @Override
                    public void onFailed(String reason) {
                        //the task has failed for several times of trying
                    }
                }).build();

        ymodem.start();
    }

    /**
     * When you received response from the ble terminal, tell ymodem
     */
    public void onDataReceivedFromBLE(byte[] data) {
        ymodem.onReceiveData(data);
    }

    /*stop the transmission*/
    public void onStopClick(View view) {
        ymodem.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //When the activity finished unexpected, just call stop ymodem
        ymodem.stop();
    }
}
