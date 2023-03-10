package com.example.water_app.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.water_app.databinding.FragmentMapBinding
import com.example.water_app.map.KakaoAPI
import com.example.water_app.map.ListAdapter
import com.example.water_app.map.LocationData
import com.example.water_app.map.ResultSearchKeyword
import com.example.water_app.model.PostData
import com.example.water_app.recyclerview.DonationAdapter
import com.example.water_app.repository.Repository
import com.example.water_app.viewmodel.MainViewModel
import com.example.water_app.viewmodel.MainViewModelFactory
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MapFragment() : Fragment() {

    private lateinit var binding: FragmentMapBinding
    private lateinit var mapView: MapView

    // 검색 RecyclerView
    private val listItems = arrayListOf<LocationData>() // 리사이클러 뷰 아이템
    private val listAdapter = ListAdapter(listItems) // 리사이클러 뷰 어댑터
    private var pageNumber = 1 // 검색 페이지 번호
    private var keyword = "" // 검색 키워드

    // 카카오 API
    companion object {
        const val BASE_URL = "https://dapi.kakao.com/"
        const val API_KEY = "KakaoAK 887bc0eaa4f3e8018acf2539644a00db" // REST API 키
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // 뷰바인딩
        binding = FragmentMapBinding.inflate(inflater, container, false)

        // 맵 띄우기
        mapView = MapView(requireActivity())
        val mapViewContainer = binding.mapLayout as ViewGroup
        mapViewContainer.addView(mapView)

        // DB 위치 표시
        val viewModel : MainViewModel
        val repository = Repository()
        val viewModelFactory = MainViewModelFactory(repository)

        viewModel = ViewModelProvider(this,viewModelFactory).get(MainViewModel::class.java)
        viewModel.getDonationList()
        viewModel.getDonationListResponse.observe(viewLifecycleOwner, Observer {
            // 통신 성공
            if(it.isSuccessful){
                val locationList : List<PostData>? = it.body()
                val dbMarker : MapPOIItem = MapPOIItem()

                for (index in 0 until locationList!!.size){
                    val cntr_loc_lat = locationList!!.get(index)!!.cntr_loc_lat!!.toDouble()
                    val cntr_loc_lng = locationList!!.get(index)!!.cntr_loc_lng!!.toDouble()
                    val cntr_rcvfvr = locationList!!.get(index)!!.cntr_rcvfvr!!.toString()

                    var MY_LOCATION = MapPoint.mapPointWithGeoCoord(cntr_loc_lat, cntr_loc_lng)

                    dbMarker.itemName = cntr_rcvfvr
                    dbMarker.tag = 0
                    dbMarker.mapPoint = MY_LOCATION

                    // 기본 마커
                    dbMarker.markerType = MapPOIItem.MarkerType.YellowPin

                    // 기본 마커 클릭했을 때 나타나는 마커
                    dbMarker.selectedMarkerType = MapPOIItem.MarkerType.RedPin

                    mapView.addPOIItem(dbMarker)

                    MY_LOCATION = null
                }
            }
        })

        // 중심점
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val lm: LocationManager =
                    requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

                // 현재 위치 받아옴
                val userNowLocation: Location =
                    lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)!!
                val uLatitude = userNowLocation.latitude
                val uLongitude = userNowLocation.longitude

                // 중심점 + 줌 레벨
                mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(uLatitude, uLongitude), 1, true)

                // 해당 위치 저장
                val MY_LOCATION = MapPoint.mapPointWithGeoCoord(uLatitude, uLongitude)

                // 마커 표시
                val marker = MapPOIItem()
                marker.itemName = "현재 위치"
                marker.tag = 0
                marker.mapPoint = MY_LOCATION

                // 기본 마커
                marker.markerType = MapPOIItem.MarkerType.BluePin

                // 기본 마커 클릭했을 때 나타나는 마커
                marker.selectedMarkerType = MapPOIItem.MarkerType.RedPin

                mapView.addPOIItem(marker)
            }
        }

        // 현재 위치로 돌아가기
        binding.btnLocation.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (requireContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val lm: LocationManager =
                        requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

                    // 현재 위치 받아옴
                    val userNowLocation: Location =
                        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)!!
                    val uLatitude = userNowLocation.latitude
                    val uLongitude = userNowLocation.longitude

                    // 중심점 + 줌 레벨
                    mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(uLatitude, uLongitude), 1, true)

                    // 해당 위치 저장
                    val MY_LOCATION = MapPoint.mapPointWithGeoCoord(uLatitude, uLongitude)

                    // 마커 표시
                    val marker = MapPOIItem()
                    marker.itemName = "현재 위치"
                    marker.tag = 0
                    marker.mapPoint = MY_LOCATION

                    // 기본 마커
                    marker.markerType = MapPOIItem.MarkerType.BluePin

                    // 기본 마커 클릭했을 때 나타나는 마커
                    marker.selectedMarkerType = MapPOIItem.MarkerType.RedPin

                    mapView.addPOIItem(marker)
                }
            }
        }

        /* 검색 기능 */
        binding.rvList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvList.adapter = listAdapter

        // 아이템 클릭 시 해당 위치로 이동
        listAdapter.setItemClickListener(object : ListAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int) {
                val mapPoint =
                    MapPoint.mapPointWithGeoCoord(listItems[position].y, listItems[position].x)
                mapView.setMapCenterPointAndZoomLevel(mapPoint, 1, true)

                binding.rvList.setVisibility(View.GONE)

                viewModel.getDonationListResponse.observe(viewLifecycleOwner, Observer {
                    // 통신 성공
                    if(it.isSuccessful){
                        val locationList : List<PostData>? = it.body()
                        val dbMarker : MapPOIItem = MapPOIItem()

                        for (index in 0 until locationList!!.size){
                            val cntr_loc_lat = locationList!!.get(index)!!.cntr_loc_lat!!.toDouble()
                            val cntr_loc_lng = locationList!!.get(index)!!.cntr_loc_lng!!.toDouble()
                            val cntr_rcvfvr = locationList!!.get(index)!!.cntr_rcvfvr!!.toString()

                            var MY_LOCATION = MapPoint.mapPointWithGeoCoord(cntr_loc_lat, cntr_loc_lng)

                            dbMarker.itemName = cntr_rcvfvr
                            dbMarker.tag = 0
                            dbMarker.mapPoint = MY_LOCATION

                            // 기본 마커
                            dbMarker.markerType = MapPOIItem.MarkerType.YellowPin

                            // 기본 마커 클릭했을 때 나타나는 마커
                            dbMarker.selectedMarkerType = MapPOIItem.MarkerType.RedPin

                            mapView.addPOIItem(dbMarker)

                            MY_LOCATION = null
                        }
                    }
                })
            }
        })

        // 검색 버튼
        binding.ivSearch.setOnClickListener {
            keyword = binding.edtSearch.text.toString()
            pageNumber = 1
            searchKeyword(keyword, pageNumber)

            binding.rvList.setVisibility(View.VISIBLE)
        }

        return binding.root
    }

    // 키워드 검색
    private fun searchKeyword(keyword: String, page: Int) {
        val retrofit = Retrofit.Builder() // Retrofit 구성
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(KakaoAPI::class.java) // 통신 인터페이스를 객체로 생성
        val call = api.getSearchKeyword(API_KEY, keyword, page) // 검색 조건 입력

        // API 서버에 요청
        call.enqueue(object : Callback<ResultSearchKeyword> {
            // 통신 성공
            override fun onResponse(
                call: Call<ResultSearchKeyword>,
                response: Response<ResultSearchKeyword>
            ) {
                addItemsAndMarkers(response.body())
            }
            // 통신 실패
            override fun onFailure(call: Call<ResultSearchKeyword>, t: Throwable) {
                // 없으면 오류 뜸
            }
        })
    }

    // 검색 결과 처리
    private fun addItemsAndMarkers(searchResult: ResultSearchKeyword?) {
        if (!searchResult?.documents.isNullOrEmpty()) {
            // 검색 결과 있음
            listItems.clear() // 리스트 초기화
            mapView.removeAllPOIItems() // 지도의 마커 모두 제거
            for (document in searchResult!!.documents) {
                // 결과를 리사이클러 뷰에 추가
                val item = LocationData(
                    document.place_name,
                    document.road_address_name,
                    document.address_name,
                    document.x.toDouble(),
                    document.y.toDouble()
                )
                listItems.add(item)

                // 지도에 마커 추가
                val point = MapPOIItem()
                point.apply {
                    itemName = document.place_name
                    mapPoint = MapPoint.mapPointWithGeoCoord(
                        document.y.toDouble(),
                        document.x.toDouble()
                    )
                    markerType = MapPOIItem.MarkerType.BluePin
                    selectedMarkerType = MapPOIItem.MarkerType.RedPin
                }
                mapView.addPOIItem(point)
            }
            listAdapter.notifyDataSetChanged()
        } else {
            // 검색 결과 없음
            Toast.makeText(requireContext(), "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 뒤로가기 메인 고정
    private lateinit var callback: OnBackPressedCallback
    lateinit var mainActivity: MainActivity

    // 뒤로가기 고정
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)

        mainActivity = context as MainActivity
    }

    // 뒤로가기 고정
    override fun onDetach() {
        super.onDetach()
        callback.remove()
    }
}