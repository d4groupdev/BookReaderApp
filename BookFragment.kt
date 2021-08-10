package com.devforfun.example.ui.book.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.devforfun.example.Prefs
import com.folioreader.PrefsReader
import com.devforfun.example.R
import com.devforfun.example.download.model.DownloadResult
import com.devforfun.example.download.utils.Extensions
import com.devforfun.example.download.utils.globalContext
import com.devforfun.example.model.authentication.socialite.user.UserInfo
import com.devforfun.example.model.bookmark.BookMarkResponce
import com.devforfun.example.model.download.Download
import com.devforfun.example.model.history.HistoryAddResponse
import com.devforfun.example.model.note.DataNote
import com.devforfun.example.model.note.Note
import com.devforfun.example.model.note.NoteListResponse
import com.devforfun.example.model.search.Book
import com.devforfun.example.model.search.BookHistory
import com.devforfun.example.model.search.favorites.SearchFavorites
import com.devforfun.example.network.NetworkManager
import com.devforfun.example.network.NetworkState
import com.devforfun.example.ui.book.adapter.NotesAdapter
import com.devforfun.example.utils.MySingleton
import com.devforfun.example.utils.getColor
import com.devforfun.example.utils.getDrawable
import com.example.jean.jcplayer.general.errors.AudioUrlInvalidException
import com.example.jean.jcplayer.model.JcAudio
import com.example.jean.jcplayer.view.JcPlayerView
import com.folioreader.Config
import com.folioreader.Constants
import com.folioreader.DataNotes
import com.folioreader.FolioReader
import com.folioreader.model.HighLight
import com.folioreader.model.locators.ReadLocator
import com.folioreader.util.AppUtil
import com.folioreader.util.OnHighlightListener
import com.folioreader.util.ReadLocatorListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.scroll_book_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.concurrent.TimeUnit

class BookFragment : Fragment(), OnHighlightListener, ReadLocatorListener,
    FolioReader.OnClosedListener, FolioReader.NoteListener, PlayerFragment.Rate {

    private var readPercent: Double = 0.0
    private var folioReader: FolioReader? = null
    private lateinit var prefs: Prefs
    private lateinit var prefsReader: PrefsReader
    private val api = NetworkManager.apiService
    private var favorites = false
    private var idBook = 0
    private var title = ""
    private lateinit var player: PlayerFragment
    private lateinit var recyclerNoteAdapter: NotesAdapter
    private var notesList: ArrayList<Note> = ArrayList()
    private var url: String = ""
    private var urlImage: String = ""
    private var urlAudio: String = ""
    private var isDownload = false
    private var status: String = ""
    private var bookMarkPosition: Int? = 0
    private var book: Book? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressScrollBook.visibility = View.VISIBLE
        MySingleton.getInstance()?.isSearch = "1"
        prefs = Prefs(requireContext())
        prefsReader = PrefsReader(requireContext())
        val navView: BottomNavigationView = requireActivity().findViewById(R.id.nav_view)
        navView.visibility = View.VISIBLE

        NetworkState.getInstance().runWhenNetworkAvailable {
            initBook()
        }.showNetworkNotAvailableMessage(getString(R.string.internet_not_available))

        player = PlayerFragment(this)
        player.playerUp(true)
        player.isActiv(false)
        folioReader = FolioReader.get()
            .setOnHighlightListener(this)
            .setReadLocatorListener(this)
            .setOnClosedListener(this)
            .setNoteListener(this)
        imageBackBook.setOnClickListener {
            Log.i("clickclick", "click")
            findNavController().popBackStack()
        }

        btn_Play.setOnClickListener {
            if (book != null) {
                prefs.putString("playerImgLarge", book!!.data.image_large)
                prefs.putString("playerImgSmall", book!!.data.image_small)
            }

            prefs.putBoolean("READ_OR_PLAY", true)

            player.playerInit(requireActivity())
            logo_book.visibility = View.VISIBLE
            player.expand()
            if (File(
                    globalContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    nameBook.text.toString()
                ).exists()
            ) {
                player.playAudio(
                    JcAudio.createFromFilePath(
                        nameBook.text.toString(),
                        File(
                            globalContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                            nameBook.text.toString()
                        ).path.toString()
                    )
                )
            } else {
                try {
                    player.playAudio(JcAudio.createFromURL(nameBook.text.toString(), urlAudio))
                } catch (ex: AudioUrlInvalidException) {
                    ex.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            player.playerUp(true)
            player.isActiv(false)
        }

        downloadButton.setOnClickListener {
            if ((prefs.getString("subscription") == "1" || prefs.getString("subscription") == "") && status != "free") {
                val action = BookFragmentDirections.actionBookFragmentToPlansFragment()
                findNavController().navigate(action)
            } else if ((prefs.getString("subscription") == "1" || prefs.getString("subscription") == "") && status == "free") {
                val action = BookFragmentDirections.actionBookFragmentToPlansFragment()
                findNavController().navigate(action)
            } else {
                prefs.putToList(
                    "Download",
                    Download(
                        nameBook.text.toString(),
                        descriptionBook.text.toString(),
                        urlImage,
                        false,
                        "5hrs 23mins",
                        0,
                        urlAudio,
                        idBook.toString()
                    )
                )
                val action = BookFragmentDirections.actionBookFragmentToNavigationLibrary()
                action.isDownload = true
                findNavController().navigate(action)
            }

        }

        ivScrollBookShare.setOnClickListener {

            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, "Share")
                val subject = "google.com"
                putExtra(Intent.EXTRA_TEXT, subject)
                type = "plain/text"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }


        openBook.setOnClickListener {
            FolioReader.readPercent = readPercent

            prefs.putBoolean("READ_OR_PLAY", false)
            prefs.putString("URL", url)

            if (File(url).exists()) {
                var config = AppUtil.getSavedConfig(requireContext())
                if (config == null) config = Config()
                config = Config()
                    .setAllowedDirection(Config.AllowedDirection.VERTICAL_AND_HORIZONTAL)
                    .setDirection(Config.Direction.VERTICAL)
                    .setFont(
                        if (prefs.getFont() == "1") {
                            Constants.FONT_ANDADA
                        } else if (prefs.getFont() == "2") {
                            Constants.FONT_LATO
                        } else if (prefs.getFont() == "3") {
                            Constants.FONT_LORA
                        } else {
                            Constants.FONT_RALEWAY
                        }
                    )
                    .setFontSize(if (prefsReader.getSize() != 0) prefsReader.getSize() else 1)
                    .setNightMode(prefsReader.getNight())

                if (book != null) {
                    FolioReader.bookName = book!!.data.title
                }

                folioReader!!.setConfig(config, true)
                    .openBook(url, bookMarkPosition!!)
                folioReader?.setBookmark(FolioReader.bookmarkChapter)
            }
        }

        isFavorite.setOnClickListener {
            favorites()

            if (favorites) {
                Glide.with(requireContext())
                    .load(R.drawable.ic_favorites_search_is_favorites)
                    .into(isFavorite)
            } else {
                Glide.with(requireContext())
                    .load(R.drawable.ic_add_to_favorites_black)
                    .into(isFavorite)
            }
        }

        tvScrollBookViewAll.setOnClickListener {
            val action = BookFragmentDirections.actionBookFragmentToAllNotes()
            findNavController().navigate(action)
        }


    }


    private fun initBook() {
        val userInfo = api.getUserInfo("Bearer ${prefs.getString("token")}")

        userInfo?.enqueue(object : Callback<UserInfo> {
            override fun onResponse(
                call: Call<UserInfo>,
                response: Response<UserInfo>
            ) {
                if (response.isSuccessful) {
                    Log.i("NetworkManager", response.body()!!.data.subscription.toString())
                    prefs.putString(
                        "subscription",
                        response.body()!!.data.subscription.is_expired.toString()
                    )

                }
            }

            override fun onFailure(call: Call<UserInfo>, t: Throwable) {
                Log.i("NetworkManager", t.message.toString())
            }

        })

        val getBook = api.getBook(
            "Bearer ${prefs.getString("token")}",
            prefs.getString("idBook")
        )

        getBook?.enqueue(object : Callback<Book> {
            override fun onResponse(call: Call<Book>, response: Response<Book>) {
//                Log.d("searchCategoryList", "response " + response.body()!!)
                if (response.body() != null && response.body()!!.success) {
                    if (response.body()!!.data.image_original.isEmpty() && context != null) {
                        Glide.with(context!!)
                            .load(R.drawable.placeholder_global_book)
                            .into(imageBook)
                    } else if (context != null) {
                        Glide.with(context!!)
                            .load(response.body()!!.data.image_original)
                            .into(imageBook)
                    }
                    title = response.body()!!.data.title
                    downloadWithFlow(
                        response.body()!!.data.reading_file,
                        response.body()!!.data.title
                    )
                    book = response.body()!!
                    JcPlayerView.author = ""
                    for (item in book!!.data.author) {
                        JcPlayerView.author += item.name + ", "
                    }
                    JcPlayerView.author =
                        JcPlayerView.author.trim().substring(0, JcPlayerView.author.length - 2)
                    idBook = response.body()!!.data.id
                    status = response.body()!!.data.status
                    Log.i(
                        "searchCategoryList",
                        "status ${prefs.getString("subscription")} +++++++ $status"
                    )
                    if (response.body()!!.data.bookmark.page > 0) {
                        bookMarkPosition = response.body()!!.data.bookmark.page
                        FolioReader.bookmarkChapter = bookMarkPosition!!
                    } else {
                        FolioReader.bookmarkChapter = -1
                    }
                    prefs.putString("bookMarkPagePosition", bookMarkPosition.toString())

                    if (response.body()!!.data.history != null) {
                        readPercent = response.body()!!.data.history!!.read_percent
                        prefs.putString("readPagePercent", readPercent.toString())
                    } else {
                        readPercent = 0.0
                        prefs.putString("readPagePercent", "0")
                    }

                    prefs.putString("is_loves", response.body()!!.data.is_loves.toString())
                    url = File(
                        globalContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        response.body()!!.data.title + ".epub"
                    ).path
                    favorites = response.body()!!.data.is_favorites
                    urlImage = response.body()!!.data.image_small
                    prefs.putString("imageBig", response.body()!!.data.image_original)
                    prefs.putString("image", urlImage)
                    prefs.putString("id", response.body()!!.data.id.toString())
                    prefs.putString("name", response.body()!!.data.title)
                    if (response.body()!!.is_likes || response.body()!!.is_dislikes) {
                        prefs.putBoolean("isBookSetRate", true)
                    } else {
                        prefs.putBoolean("isBookSetRate", false)
                    }

                    urlAudio = response.body()!!.data.audio_file
                    nameBook?.text = response.body()!!.data.title

                    authorBook?.text = JcPlayerView.author

                    descriptionBook?.text = response.body()!!.data.description
                    likeCount?.text = response.body()!!.data.likes.toString()
                    dislikeCount?.text = response.body()!!.data.dislikes.toString()

                    if (extension.checkIsFileExist(response.body()!!.data.title)
                    ) {
                        downloadButton?.background =
                            getDrawable(R.drawable.background_button_download_active)
                        downloadButton?.setTextColor(getColor(R.color.colorForgotPassword))

                        //downloadButton?.isEnabled = false
                    }
                    if (response.body()!!.data.is_favorites) {
                        Glide.with(requireContext())
                            .load(R.drawable.ic_favorites_search_is_favorites)
                            .into(isFavorite)
                    } else {
                        Glide.with(requireContext())
                            .load(R.drawable.ic_add_to_favorites_black)
                            .into(isFavorite)
                    }
                    prepareNotesList()
                }
            }


            override fun onFailure(call: Call<Book>, t: Throwable) {
                progressScrollBook.visibility = View.GONE
            }

        })

    }

    private fun formatAudioTime(timeInMs: Float): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMs.toLong())
        var str = ""
        str += if (minutes < 10) "0$minutes:" else "$minutes:"
        val seconds = ((timeInMs / 1000).toInt()) - minutes * 60
        str += if (seconds < 10) "0$seconds" else "$seconds"
        return str
    }

    private fun sendBookmark(bookMark: Int) {
        val bookmark = api.bookmark(
            "Bearer ${prefs.getString("token")}",
            prefs.getString("idBook").toInt(),
            BookMarkResponce(bookMark.toString())
        )

        bookmark?.enqueue(object : Callback<Book> {
            override fun onResponse(call: Call<Book>, response: Response<Book>) {
                progressScrollBook.visibility = View.GONE
            }

            override fun onFailure(call: Call<Book>, t: Throwable) {
                progressScrollBook.visibility = View.GONE
            }

        })
    }

    private val extension: Extensions = Extensions()

    private fun downloadWithFlow(url: String, nameBook: String) {
        CoroutineScope(Dispatchers.IO).launch {
            extension.downloadFile(
                File(
                    globalContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "$nameBook.epub"
                ), url
            ).collect {
                withContext(Dispatchers.Main) {
                    when (it) {
                        is DownloadResult.Success -> {
                            isDownload = true
                        }
                        is DownloadResult.Error -> {
                            isDownload = false
                            Toast.makeText(
                                requireContext(),
                                "${resources.getText(R.string.error_while_downloading)}$nameBook",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is DownloadResult.Progress -> {

                        }
                    }
                }
            }
        }
    }

    fun favorites() {
        favorites = !favorites
        val isFavor = if (favorites) 1 else 0
        val favorite = api.searchFavorites(
            "Bearer ${prefs.getString("token")}",
            idBook.toString(),
            isFavor
        )

        favorite?.enqueue(object : Callback<SearchFavorites> {
            override fun onResponse(
                call: Call<SearchFavorites>,
                response: Response<SearchFavorites>
            ) {
                Log.d("mytag_search_favor", "response " + response.body().toString())
                if (response.body()!!.success) {
                    Log.d("mytag_search_favor", "good")
                }
            }

            override fun onFailure(call: Call<SearchFavorites>, t: Throwable) {
                Log.d("mytag_search_favor", "t $t")
            }
        })
    }

    fun updateTimeAndPage() {
        val getBook = api.getBook(
            "Bearer ${prefs.getString("token")}",
            prefs.getString("idBook")
        )

        getBook?.enqueue(object : Callback<Book> {
            override fun onFailure(call: Call<Book>, t: Throwable) {
                Log.d("searchCategoryList", "t $t")
                progressScrollBook?.visibility = View.GONE
            }

            override fun onResponse(call: Call<Book>, response: Response<Book>) {
                if (response.body() != null && response.body()!!.data.history != null)
                    if (response.body()!!.data.history != null) {
                        book = response.body()!!
                        activity?.runOnUiThread {
                            openBook?.text =
                                getString(R.string.page) + " " + book!!.data.history!!.pageRead!!.toInt()
                            btn_Play?.text = formatAudioTime(book!!.data.history!!.timeAudio * 1000)
                        }
                    }
            }
        })
    }

    private fun prepareNotesList() {
        recyclerNoteAdapter = NotesAdapter(notesList)
        rvScrollBookNotes.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recyclerNoteAdapter
        }

        val retrofitGetNotes =
            NetworkManager.apiService.getBookNotes("Bearer ${prefs.getString("token")}", idBook)
        retrofitGetNotes.enqueue(object : Callback<NoteListResponse> {
            override fun onResponse(
                call: Call<NoteListResponse>,
                response: Response<NoteListResponse>
            ) {
                Log.i("NetworkManager", "Notes body  ${response.body().toString()}")

                if (response.body() != null)
                    if (response.body()!!.success) {
                        notesList.clear()
                        notesList.addAll(response.body()!!.noteData)
                        recyclerNoteAdapter.notifyDataSetChanged()
                        activity?.runOnUiThread {
                            progressScrollBook.visibility = View.GONE
                        }
                    }

            }

            override fun onFailure(call: Call<NoteListResponse>, t: Throwable) {
                Log.i("NetworkManager", "Notes error  ${t.localizedMessage}")
                progressScrollBook.visibility = View.GONE
            }

        })
    }

    override fun onHighlight(highlight: HighLight?, type: HighLight.HighLightAction?) {
        Log.i("readerBook", highlight!!.pageNumber.toString() + "      hoigfjadk")
    }

    override fun saveReadLocator(readLocator: ReadLocator?) {
        Log.i("readerBook", readLocator!!.href)
    }

    override fun onFolioReaderClosed() {
        Log.i("readerBook", "read")
    }

    override fun onDestroy() {
        super.onDestroy()
        FolioReader.clear()
    }

    override fun onPause() {
        super.onPause()
        player.playerUp(false)
        player.isActiv(true)
        player.createNotification()
    }

    override fun onStop() {
        super.onStop()
        player.createNotification()
    }

    override fun getNote(note: DataNotes) {
        val favorite = api.createBookNote(
            "Bearer ${prefs.getString("token")}",
            prefs.getString("idBook").toInt(),
            note
        )

        favorite?.enqueue(object : Callback<DataNote> {
            override fun onResponse(
                call: Call<DataNote>,
                response: Response<DataNote>
            ) {
                if (response.body()!!.success) {
                    Log.d("mytag_search_favor", "good")
                }
            }

            override fun onFailure(call: Call<DataNote>, t: Throwable) {
                Log.d("mytag_search_favor", "t $t")
            }
        })
    }

    override fun onGoToNotes() {
        Handler().postDelayed({
            activity?.runOnUiThread {
                val action = BookFragmentDirections.actionBookFragmentToAllNotes()
                findNavController().navigate(action)
            }
        }, 100)
    }

    override fun onResume() {
        super.onResume()
        if (prefs.getBoolean("BOOK_IS_READ")) {
            prefs.putBoolean("BOOK_IS_READ", false)
            val slidingUpPanelLayout =
                activity?.findViewById<SlidingUpPanelLayout>(R.id.sliding_layout)
            slidingUpPanelLayout?.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
            activity?.findViewById<ConstraintLayout>(R.id.app_bar_expand)?.visibility =
                View.VISIBLE

            activity?.findViewById<ConstraintLayout>(R.id.player_root)?.visibility = View.GONE
            activity?.findViewById<ImageButton>(R.id.btnPlay)?.visibility = View.GONE

            if (player.player != null) {
                player.player!!.pause()
            }

            NetworkState.getInstance().runWhenNetworkAvailable {
                updateTimeAndPage()
            }.showNetworkNotAvailableMessage(getString(R.string.internet_not_available))
        }
    }

    override fun getReadLocation(position: Int?) {

        if (this.bookMarkPosition == position!!)
            return
        this.bookMarkPosition = position

        val readPercent: Int =
            ((position.toDouble() / FolioReader.chapterCount.toDouble()) * 100).toInt()
        prefs.putString("readPagePercent", readPercent.toString())
        Log.d(
            "BookFragment",
            "getReadLocation: ${position}, readPercent: ${readPercent}, chapterCount: ${FolioReader.chapterCount}"
        )

        prefs.putString("page", position.toString())
        var time = if (prefs.getString("player") != "") prefs.getString("player").toInt() else 0
        val addToHistory = api.addToHistoryRead(
            "Bearer ${prefs.getString("token")}",
            prefs.getString("idBook").toInt(),
            position.toFloat() + 1,
            0,
            (position.toDouble() / prefs.getString("countPage").toDouble()) * 100,
//            readPercent,
            0
        )

        addToHistory.enqueue(object : Callback<HistoryAddResponse> {
            override fun onResponse(
                call: Call<HistoryAddResponse>,
                response: Response<HistoryAddResponse>
            ) {
                Log.i("folioReader", "$response ------ your page")
                if (response.body() != null) {
                    Log.d(
                        "Folio",
                        "Add to history ${response.body()!!.message} percent : ${readPercent}"
                    )
                }
            }

            override fun onFailure(call: Call<HistoryAddResponse>, t: Throwable) {
                Log.e("Folio", "History send on server error: ${t.localizedMessage}")
            }
        })

    }

    override fun getPageCount(count: Int?) {
        Log.i("folioReader", "$count ------ count page")
        prefs.putString("countPage", count.toString())
        prefs.putToListHistory("pageHistory", BookHistory(prefs.getString("name"), count!!))

    }

    override fun saveBookmark() {
        if (bookMarkPosition != null) {
            sendBookmark(bookMarkPosition!!)
            FolioReader.bookmarkChapter = bookMarkPosition!!
        }
    }

    override fun onEndRead() {
        progressScrollBook.visibility = View.VISIBLE
        Handler().postDelayed({
            activity?.runOnUiThread {
                progressScrollBook.visibility = View.GONE
            }
            if (prefs.getString("is_loves") != "true") {
                val action = BookFragmentDirections.actionBookFragmentToRateBookFragment(
                    book!!.data.title,
                    book!!.data.id
                )
                findNavController().navigate(action)
            }

        }, 100)
        val retrofitEndRead = NetworkManager.apiService.setBookIsReadEnd(
            "Bearer ${prefs.getString("token")}",
            book!!.data.id,
            1
        )
        retrofitEndRead.enqueue(object : Callback<Void> {
            override fun onResponse(
                call: Call<Void>,
                response: Response<Void>
            ) {
                Log.i("Book", "Book is end read sended success")
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.i("Book", "Book is end read sended error: ${t.localizedMessage}")
            }
        })
    }

    override fun onListen() {
        btn_Play.performClick()
    }

    override fun onClickRate(click: Boolean) {
    }
}
