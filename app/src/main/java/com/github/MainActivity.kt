package com.github

//import kotlinx.android.synthetic.main.activity_main.*

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {
    //создаем массивы, в котором будут храниться данные о логине пользователя
    var userArray: MutableList<String> = mutableListOf()
    //создаем массивы, в котором будут храниться данные о логине пользователя при недоступности сервера
    var userArrayOffLine: MutableList<String> = mutableListOf()
    //создаем массивы, в котором будут храниться данные о ID пользователя
    var userIdArray: MutableList<String> = mutableListOf()
    //создаем массивы, в котором будут храниться данные о ID пользователя при недоступности сервера
    var userIdArrayOffLine: MutableList<String> = mutableListOf()
    //создаем массивы, в котором будут храниться данные о аватаре пользователя
    var userAvatarArray: MutableList<String> = mutableListOf()
    //создаем массивы, в котором будут храниться данные о аватаре пользователя при недоступности сервера
    var userAvatarArrayOffLine: MutableList<String> = mutableListOf()
    var errorRetrofit: Boolean = false
    var pageNumber: Int = 1
    var pageNumberOnPause: Int = 0
    //переменная, в которой будет хранится данные о ввдееном логине пользователя для поиска
    var userSearch: String = ""

    // создаем список для отображения данных из массива userArray
    var listUserView: ListView? = null


    // переменная для хранения ссылки к API серверу
    val baseUrl = "https://api.github.com/"

    //данная константа необходима для работы с API без использования okhttp
    //val token = "bearer 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"

    val token = "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"

    //иниуиализация переменных для элементов Активити
    lateinit var buttonSearch: Button
    lateinit var editSearch: TextView
    lateinit var buttonNext: Button
    lateinit var buttonPreview: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        //устанавливаем другую тему
        setTheme(R.style.Github)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listUserView = findViewById(R.id.listUserView)
        buttonSearch = findViewById(R.id.buttonSearch)
        editSearch = findViewById(R.id.editSearch)
        buttonNext = findViewById(R.id.buttonNext)
        buttonPreview = findViewById(R.id.buttonPreview)
        listUserView?.isClickable = false
        //обработка нажатия на выбранный элемент ListView
        listUserView?.setOnItemClickListener { parent, view, position, id ->

            //создание интента для новой активности
            if (errorRetrofit) {

                errorData()
            } else {
                val intent = Intent(this@MainActivity, InfoActivity::class.java)
                //передача данных в другую активность
                intent.putExtra("userSearch", userSearch)
                intent.putExtra("login", userArray[id.toInt()])
                intent.putExtra("id", userIdArray[id.toInt()])
                intent.putExtra("avatar", userAvatarArray[id.toInt()])

                //запуск новой активности
                startActivity(intent)
            }
        }
    }

    //создаем функцию для подключения к сайту github.com - она не работает, так как github удалаяет токены, которые опубликованы в репозиториях.
    var okHttpClient: OkHttpClient? = OkHttpClient().newBuilder().addInterceptor { chain ->
        val originalRequest: Request = chain.request()
        val builder: Request.Builder = originalRequest.newBuilder().header(
            "Authorization",
            Credentials.basic("s_fortis@mail.ru", token)
        )
        val newRequest: Request = builder.build()
        chain.proceed(newRequest)
    }.build()


    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        // .client(okHttpClient)
        .build()

    fun userRetrofit() {
        var client: InterfaceAPI = retrofit.create(InterfaceAPI::class.java)
        val call: Call<Users<Item>> = client.getLoginUser("$userSearch in:login", pageNumber)
        call.enqueue(object : Callback<Users<Item>> {

            override fun onResponse(
                call: Call<Users<Item>>,
                response: Response<Users<Item>>
            ) {
                if (!response.isSuccessful) {
                    println("code: " + response.code())
                    userArray = userArrayOffLine.toMutableList()
                    userIdArray = userIdArrayOffLine.toMutableList()
                    userAvatarArray = userAvatarArrayOffLine.toMutableList()
                    errorData()
                    pageNumber = pageNumberOnPause
                    if (userArray.isEmpty()) {
                        buttonPreview.setVisibility(View.INVISIBLE)
                        buttonNext.setVisibility(View.INVISIBLE)
                    }

                    return
                }
                if (pageNumber == 1) buttonNext.setVisibility(View.VISIBLE)
//                val ugit: Users<Item>? = response.body()
//                for (i in 0 until (ugit?.items?.size!!)) {
//                    userArray.add(ugit.items[i].loginUser)
//                    userIdArray.add(ugit.items[i].id)
//                    userAvatarArray.add(ugit.items[i].avatarUrl)
//                }
//                if (ugit?.items?.size!! < 30) {
//                    buttonNext.setVisibility(View.INVISIBLE)
//                }
                val users = response.body()?.items.orEmpty()
                users.forEach {
                    userArray.add(it.loginUser)
                    userIdArray.add(it.id)
                    userAvatarArray.add(it.avatarUrl)
                }
                if (users.size < 30) {
                    buttonNext.setVisibility(View.INVISIBLE)
                }


                userArrayOffLine = userArray.toMutableList()
                userIdArrayOffLine = userIdArray.toMutableList()
                userAvatarArrayOffLine = userAvatarArray.toMutableList()
                addLogInArray()
            }

            override fun onFailure(call: Call<Users<Item>>, t: Throwable) {
                println(t)
            }
        }
        )
    }

    //объявляем функцию для создания и отображения списка логинов с данными из массива userArray
    fun addLogInArray() {
        if (userArray.isEmpty()) {
            // создаем адаптер списка логинов с данными массива userArray
            val adapter = object : ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1, userArray
            ) {
            }
            //присваеваем элементам списка выше созданый адаптер
            listUserView?.adapter = adapter
        } else {
            // создаем адаптер списка логинов с данными массива userArray
            val adapter = object : ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1, userArrayOffLine
            ) {
            }
            //присваеваем элементам списка выше созданый адаптер
            listUserView?.adapter = adapter
            if (pageNumber == 1) buttonPreview.setVisibility(View.INVISIBLE)
        }
    }

    //объявляем функцию обработки нажатия на кнопку buttonSearch - поиск для введеного логина на сервере github
    fun onClickSearch(view: View) {
        userSearch = editSearch.text.toString()
        pageNumber = 1
        clearAllArrayAndStartRetrofit()
    }

    fun onClickNext(view: View) {
        if (!errorRetrofit){
            userSearch = editSearch.text.toString()
            if (pageNumber >= 1) buttonPreview.setVisibility(View.VISIBLE) else buttonPreview.setVisibility(View.INVISIBLE)
            pageNumberOnPause = pageNumber
            pageNumber++
            clearAllArrayAndStartRetrofit()
        }
    }

    //объявляем функцию для обработки нажатия на клавишу Next
    fun onClickPreview(view: View) {
        if (!errorRetrofit){
            userSearch = editSearch.text.toString()
            if (pageNumber > 1) {
                pageNumberOnPause = pageNumber
                pageNumber--
                buttonNext.setVisibility(View.VISIBLE)
            }
            clearAllArrayAndStartRetrofit()
        }
    }

    //объявляем функцию для выведения Тоаста в случае превышения лимита подключений.
    fun errorData() {
        Toast.makeText(
            this,
            "Waiting data from server. Please wait few seconds.",
            Toast.LENGTH_SHORT
        ).show()
    }

    //объявляем функцию для очистки всех массссивов для он-лайн работы, а так же запуск функции retrofit()
    fun clearAllArrayAndStartRetrofit() {
        userArray.clear()
        userIdArray.clear()
        userAvatarArray.clear()
        userRetrofit()
    }
}








