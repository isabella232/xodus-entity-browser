package jetbrains.xodus.browser.web

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.server.engine.embeddedServer
import io.ktor.server.jetty.Jetty
import io.ktor.server.jetty.JettyApplicationEngine
import jetbrains.exodus.entitystore.PersistentEntityStoreImpl
import jetbrains.exodus.entitystore.PersistentEntityStores
import jetbrains.exodus.env.Environments
import jetbrains.xodus.browser.web.db.DatabaseService
import jetbrains.xodus.browser.web.db.Databases
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Before
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


open class TestSupport {
    companion object : KLogging() {

        val mapper = ObjectMapper().also {
            it.registerModule(KotlinModule())
            it.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }

    protected val key = "teamsysdata"
    protected val lockedDBLocation = newLocation()
    private lateinit var store: PersistentEntityStoreImpl
    private val databaseService = DatabaseService()

    private lateinit var server: JettyApplicationEngine
    private val context = "/custom"
    private val port = 18443

    protected val retrofit: Retrofit by lazy {
        val client = with(OkHttpClient().newBuilder()) {
            interceptors().add(HttpLoggingInterceptor(HttpLoggingInterceptor.Logger { logger.info { it } }).apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            build()
        }
        Retrofit.Builder().client(client).baseUrl("http://localhost:$port/custom/").addConverterFactory(JacksonConverterFactory.create(mapper)).build()
    }

    protected val dbsResource by lazy {
        retrofit.create(DBsApi::class.java)
    }
    protected val dbResource by lazy {
        retrofit.create(DBApi::class.java)
    }

    protected fun newLocation(): String {
        return "java.io.tmpdir".system() + File.separator + Random().nextLong()
    }

    @Before
    fun before() {
        System.setProperty("recent.dbs", newLocation() + File.separator + "recent.dbs.json")
        store = PersistentEntityStores.newInstance(Environments.newInstance(lockedDBLocation), key)
        Application.start()
        server = embeddedServer(Jetty, port = port) {
            HttpServer(context).setup(this)
        }
        server.start(wait = false)

        var setuped = false
        var times = 0
        while (!setuped && times < 10) {
            Thread.sleep(500)
            times++
            val response = dbsResource.all().execute()
            setuped = response.isSuccessful
        }
        if (!setuped) {
            throw IllegalStateException("http server is down")
        }
    }

    fun newDB(location: String, isOpened: Boolean = false): DBSummary {
        return dbsResource.new(DBSummary(location = location,
                key = key,
                isOpened = isOpened,
                isReadonly = false,
                isWatchReadonly = false)).execute().body()!!
    }


    @After
    fun after() {
        store.close()
        databaseService.deleteAll()
        Databases.close()
        File("db").delete()
        server.stop(gracePeriod = 20, timeout = 20, timeUnit = TimeUnit.SECONDS)
    }
}