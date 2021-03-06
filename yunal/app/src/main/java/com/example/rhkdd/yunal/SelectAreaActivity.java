package com.example.rhkdd.yunal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rhkdd.yunal.adapter.AreaRVAdapter;
import com.example.rhkdd.yunal.common.RetrofitTourClient;
import com.example.rhkdd.yunal.model.areaCode.AreaCode;
import com.example.rhkdd.yunal.model.areaCode.AreaCodeItem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;

import es.dmoral.toasty.Toasty;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.rhkdd.yunal.common.RetrofitTourClient.API_key;


public class SelectAreaActivity extends AppCompatActivity {

    public static final String AREA_CODE = "AREA_CODE";
    public static final String SIGUNGU_CODE = "SIGUNGU_CODE";
    public static final String AREA_NAME = "AREA_NAME";
    public static final String SIGUNGU_NAME = "SIGUNGU_NAME";

    private ArrayList<AreaCodeItem> areaCodeLists;
    private ArrayList<AreaCodeItem> sigunguCodeLists;

    private RecyclerView areaRV;
    private RecyclerView sigunguRV;

    private AreaRVAdapter areaRVAdapter;
    private SigunguRVAdapter sigunguRVAdapter;

    private TextView allViewBtn;
    private ProgressBar sigunguLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectarea);

        Initialize();
    }

    private void Initialize() {
        // 툴바 초기화 셋팅
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_green);

        // 시군구 로딩
        sigunguLoading = findViewById(R.id.sigungu_loading);
        sigunguLoading.setVisibility(View.GONE);

        // 지역, 시군구 객체 초기화
        areaCodeLists = new ArrayList<>();
        sigunguCodeLists = new ArrayList<>();


        // recyclerview 초기화 셋팅
        areaRV = findViewById(R.id.areaRV);
        areaRV.setLayoutManager(new LinearLayoutManager(SelectAreaActivity.this));
        areaRVAdapter = new AreaRVAdapter(SelectAreaActivity.this);
        areaRVAdapter.setOnLoadAreaCodeListener(new AreaRVAdapter.OnLoadAreaCodeListener() {
            @Override
            public void onLoad(String areaCode) {
                loadAreaCodeData(areaCode);
            }
        });

        areaRV.setAdapter(areaRVAdapter);

        sigunguRV = findViewById(R.id.sigunguRV);
        sigunguRV.setLayoutManager(new LinearLayoutManager(SelectAreaActivity.this));
        sigunguRVAdapter = new SigunguRVAdapter(SelectAreaActivity.this);
        sigunguRV.setAdapter(sigunguRVAdapter);

        // 초기 큰 지명 호출
        loadAreaCodeData(null);

        //전체 보기 버튼
        allViewBtn = findViewById(R.id.allViewBtn);
        allViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String areaCode = areaRVAdapter.getAreaCode();
                String areaName = areaRVAdapter.getAreaName();

                Intent intent = SelectAreaResultActivity.newIntent(SelectAreaActivity.this, areaCode, null, areaName, null);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

            }
        });
    }

    @Override
    protected void onPause() { // 다른 액티비티 전환시 현재 액티비티 종료
        super.onPause();
        finish();
    }

    public void loadAreaCodeData(final String areaCode) {

        if (areaCode != null) {
            sigunguLoading.setVisibility(View.VISIBLE);
        }

        SelectAreaActivity.ItemDeserializer itemDeserializer = new SelectAreaActivity.ItemDeserializer();
        Gson gson = new GsonBuilder().registerTypeAdapter(AreaCode.Items.class, itemDeserializer).create();

        Call<AreaCode> call = RetrofitTourClient.getInstance().getService(gson).AreaCode(API_key, "yunal",
                "AND", "json", 1, 40, areaCode);

        call.enqueue(new Callback<AreaCode>() {
            @Override
            public void onResponse(Call<AreaCode> call, Response<AreaCode> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AreaCode areaCodeObject = response.body();
                    if (areaCodeObject != null) {
                        if (areaCode == null) { // 광역시 (큰 도시 ) 부분
                            areaCodeLists.clear();
                            areaCodeLists.addAll(areaCodeObject.response.body.items.item);
                            areaRVAdapter.setData(areaCodeLists);
                        } else { // 시군구 부분
                            sigunguCodeLists.clear();
                            sigunguCodeLists.addAll(areaCodeObject.response.body.items.item);
                            sigunguRVAdapter.setData(sigunguCodeLists);
                            sigunguLoading.setVisibility(View.GONE);
                        }

                    }
                }

            }

            @Override
            public void onFailure(Call<AreaCode> call, Throwable t) {
                Toasty.error(getApplicationContext(), "지역정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }



    public class SigunguRVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context context;
        private ArrayList<AreaCodeItem> lists;


        SigunguRVAdapter(Context context) {
            this.context = context;
            this.lists = new ArrayList<>();
        }

        void setData(ArrayList<AreaCodeItem> lists) {
            this.lists.clear();
            this.lists.addAll(lists);
            notifyDataSetChanged();
        }


        class SigunguVH extends RecyclerView.ViewHolder implements View.OnClickListener{

            TextView sigunguName;

            SigunguVH(View itemView) {
                super(itemView);

                sigunguName = itemView.findViewById(R.id.sigunguName);

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                String areaCode = areaRVAdapter.getAreaCode();
                String sigunguCode = lists.get(getAdapterPosition()).code;
                String areaName = areaRVAdapter.getAreaName();
                String sigunguName = lists.get(getAdapterPosition()).name;

                Intent intent = SelectAreaResultActivity.newIntent(SelectAreaActivity.this, areaCode, sigunguCode, areaName, sigunguName);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);


            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new SigunguVH(LayoutInflater.from(context).inflate(R.layout.item_recyclerview_sigungu, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            SigunguVH sigunguVH = (SigunguVH) holder;

            sigunguVH.sigunguName.setText(lists.get(position).name);

        }

        @Override
        public int getItemCount() {
            return lists.size();
        }
    }

    public  class ItemDeserializer implements JsonDeserializer<AreaCode.Items> {

        @Override
        public AreaCode.Items deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            try {
                JsonElement monumentElement = json.getAsJsonObject().get("item");
                if (monumentElement.isJsonArray()) { // array 일 경우
                    return new AreaCode.Items((AreaCodeItem[]) context.deserialize(monumentElement.getAsJsonArray(), AreaCodeItem[].class));
                } else if (monumentElement.isJsonObject()) { // object 일 경우
                    return new AreaCode.Items((AreaCodeItem) context.deserialize(monumentElement.getAsJsonObject(), AreaCodeItem.class));
                } else {
                    throw new JsonParseException("Unsupported type of monument element");
                }
            } catch (IllegalStateException e) {
                return null;
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
