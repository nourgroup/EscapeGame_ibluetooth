package fr.mastergime.meghasli.escapegame.ui.fragments

import android.animation.Animator
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.MediaPlayer
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.os.CountDownTimer
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import fr.mastergime.meghasli.escapegame.R
import fr.mastergime.meghasli.escapegame.databinding.FragmentGameBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import fr.mastergime.meghasli.escapegame.Notifications.createChannel
import fr.mastergime.meghasli.escapegame.Notifications.sendNotificationUpdateDone
import fr.mastergime.meghasli.escapegame.model.*
import fr.mastergime.meghasli.escapegame.services.BluetoothService
import fr.mastergime.meghasli.escapegame.viewmodels.BluetoothViewModel
import fr.mastergime.meghasli.escapegame.viewmodels.EnigmesViewModel
import fr.mastergime.meghasli.escapegame.viewmodels.SessionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.math.log

@AndroidEntryPoint
class GameFragment : Fragment(), NfcAdapter.ReaderCallback {

    val sessionViewModel: SessionViewModel by viewModels()
    val enigmeViewModel: EnigmesViewModel by viewModels()
    /*****bluetooth*****/
    private val bluetoothViewModel: BluetoothViewModel by activityViewModels()
    /*****end bluetooth*****/

    private val job = SupervisorJob()
    private val ioScope by lazy { CoroutineScope(job + Dispatchers.Main) }


    var mNfcAdapter: NfcAdapter? = null
    private lateinit var binding: FragmentGameBinding

    var enigme1State = false
    var enigme2State = false
    var enigme3State = false
    var enigme4State = false

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGameBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.intro_jeux)


        enigmeViewModel.updateEnigme1State(RoomSessionFragment.sessionId)
        enigmeViewModel.updateEnigme2State(RoomSessionFragment.sessionId)
        enigmeViewModel.updateEnigme3State(RoomSessionFragment.sessionId)
        enigmeViewModel.updateEnigme4State(RoomSessionFragment.sessionId)
        //enigmeViewModel.updateEnigme1State(RoomSessionFragment.sessionId)

        disableStatusBar()
        sessionViewModel.updateSessionId()
        sessionViewModel.starTimerSession()

        sessionViewModel.endTime.observe(viewLifecycleOwner) { value ->
            Log.d("valueTime", "mainTimer: $value ")
            mainTimer(value)
        }

        binding.quitButton.setOnClickListener {
            binding.quitButton.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
            it.isEnabled = false
            sessionViewModel.quitSession()
            /***code bluetooth**/
            try{
                //arreter le service si user quitte la session
                activity?.stopService(Intent(context, BluetoothService::class.java))
            }catch(ex : Exception ){
                Toast.makeText(context, requireContext().getString(R.string.error), Toast.LENGTH_SHORT).show()
            }
            /***end code bluetooth**/
        }

        sessionViewModel.quitSessionState.observe(viewLifecycleOwner) { value ->
            observeSessionState(value)
        }

        /*sessionViewModel.sessionId.observe(viewLifecycleOwner) {
            sessionId = it
        }*/

        enigmeViewModel.enigme1State.observe(viewLifecycleOwner, Observer {
            if (it) {
                enigme1State = true
                createListEnigmaAdapter()
            }
        })
        enigmeViewModel.enigme2State.observe(viewLifecycleOwner, Observer {
            if (it) {
                enigme2State = true
                createListEnigmaAdapter()
            }
        })
        enigmeViewModel.enigme3State.observe(viewLifecycleOwner, Observer {
            if (it) {
                enigme3State = true
                createListEnigmaAdapter()
            }
        })
        enigmeViewModel.enigme4State.observe(viewLifecycleOwner, Observer {
            if (it) {
                enigme4State = true
                createListEnigmaAdapter()
            }
        })

        createListEnigmaAdapter()
        //createListCluesAdapter()

        /********bluetooth********/
        /*
        Create channel for notification
*/

        /*val notificationManager = ContextCompat.getSystemService(
            requireContext(),
            NotificationManager::class.java
        )*/

        /*
        * Declencher quand le client reçoit un message depuis le serveur par broadcast receiver
        * */
        bluetoothViewModel.notification.observe(viewLifecycleOwner, {
            /*
                Send notification
            */

            //notificationManager?.sendNotificationUpdateDone(requireContext(),"Escape Game",it)
        })
        activity?.registerReceiver(receiver, IntentFilter("MESSAGE_FROM_SERVER"))
        /********end bluetooth********/
    }

    private fun mainTimer(endTime: Long) {
        val endTime = endTime
        val current = System.currentTimeMillis()
        Log.d("currentTime", "mainTimer: $current -> $endTime ")
        var stay = endTime - current
        Log.d("stayTime", "mainTimer: $stay ")

        object : CountDownTimer(90000, 1000) {
            override fun onTick(p0: Long) {
                stay = p0
                val minute = stay / 60000
                val second = stay % 60000 / 1000
                if (minute < 10) {
                    if (second < 10) {
                        "0$minute:0$second".also {
                            binding.textViewTime.text = it
                        }
                    } else {
                        "0$minute:$second".also {
                            binding.textViewTime.text = it
                        }
                    }
                } else if (second < 10) {
                    "$minute:0$second".also {
                        binding.textViewTime.text = it
                    }
                } else {
                    "$minute:$second".also {
                        binding.textViewTime.text = it
                    }
                }

                if (minute < 1) {
                    binding.textViewTime.setTextColor(Color.RED);
                }
            }

            override fun onFinish() {
                Toast.makeText(requireContext(), "Game Over", Toast.LENGTH_SHORT).show()
                "GAME OVER".also {
                    binding.textViewTime.text = it
                    binding.textViewTime.setTextColor(Color.RED);
                }
            }
        }.start()

    }

    private fun observeSessionState(value: String?) {
        if (value == "Success") {
            binding.quitButton.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
            sessionViewModel.notReadyPlayer()
            findNavController().navigate(R.id.action_gameFragment_to_menuFragment)
        } else
            Toast.makeText(
                activity, "Can't leave Session please retry",
                Toast.LENGTH_SHORT
            ).show()
        binding.progressBar.visibility = View.INVISIBLE
        binding.quitButton.visibility = View.VISIBLE
        binding.quitButton.isEnabled = true
    }

    private fun createListEnigmaAdapter() {
        val enigmaList = mutableListOf(
            EnigmeRecyclerObject("Enigme Optionel", false, "indice opt"),
            EnigmeRecyclerObject("Enigme One", enigme1State, "indice1"),
            EnigmeRecyclerObject("Enigme Two: Part One", enigme2State, "indice2"),
            EnigmeRecyclerObject("Enigme Two: Part Two", enigme2State, "indice2"),
            EnigmeRecyclerObject("Enigme Three", enigme3State, "indice3"),
            EnigmeRecyclerObject("Enigme Final", enigme4State, "indice final")
        )
        val enigmaListAdapter = EnigmaListAdapter {
            when (it) {
                0 -> ioScope.launch {
                    if (!enigmeViewModel.getOptionalEnigmeOpenClos())
                        findNavController().navigate(R.id.action_gameFragment_to_optionel_enigme_fragment)
                    else
                        Toast.makeText(
                            requireContext(),
                            "Enigma Already Done",
                            Toast.LENGTH_SHORT
                        ).show()
                }
                1 -> {
                    loadAnimationSignUpDone("enigme1")
                }
                //  1 -> findNavController().navigate(R.id.action_gameFragment_to_enigme1Fragment)
                2 -> {
                    loadAnimationSignUpDone("enigme21")
                }
                3 -> {
                    loadAnimationSignUpDone("enigme22")
                }
                4 -> findNavController().navigate(R.id.action_gameFragment_to_enigme3Fragment)
                5 -> findNavController().navigate(R.id.action_gameFragment_to_enigme4Fragment)
            }
        }
        enigmaListAdapter.submitList(enigmaList)
        binding.recyclerEnigma.apply {
            setHasFixedSize(true)
            adapter = enigmaListAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    /*private fun createListCluesAdapter(){
        val clueList = mutableListOf(
            UserForRecycler("Clue One",false,null),
            UserForRecycler("Clue Two",false,null),
            UserForRecycler("Clue Three",false,null),
            UserForRecycler("Clue Four",false,null)
        )

        val cluesListAdapter = ClueListAdapter()
        cluesListAdapter.submitList(clueList)
        binding.recyclerViewClues.apply {
            setHasFixedSize(true)
            adapter = cluesListAdapter
            layoutManager = CenterZoomLayoutManager(context,LinearLayoutManager.HORIZONTAL,false)
        }
    }*/

    private fun enableNfc() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context)
        if (mNfcAdapter != null && mNfcAdapter!!.isEnabled) {

            mNfcAdapter!!.enableReaderMode(
                this.activity,
                this,
                NfcAdapter.FLAG_READER_NFC_A, Bundle.EMPTY
            )
        } else {
        }
    }

    override fun onTagDiscovered(tag: Tag?) {

        val mNdef: Ndef? = Ndef.get(tag)

        if (mNdef != null) {
            mNdef.connect()
            val mNdefMessage = mNdef.ndefMessage
            val msg = mNdefMessage.records[0].toUri().toString()

            // msg = msg du tag
            lifecycleScope.launch(Dispatchers.Main) {
                loadAnimationSignUpDone(msg)
            }

            mNdef.close()
        } else {
            ReaderMode.message = "FAILED"
        }
    }

    private fun disableStatusBar() {
        (activity as AppCompatActivity).supportActionBar?.hide()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }


    override fun onResume() {
        super.onResume()
        enableNfc()
        disableStatusBar()
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    override fun onPause() {
        super.onPause()
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context)
        if (mNfcAdapter != null && mNfcAdapter!!.isEnabled) {
            mNfcAdapter!!.disableReaderMode(activity)
        }

        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }

    }

    companion object {
        var sessionId = ""
    }

    private fun loadAnimationSignUpDone(enigmeTag: String) {
        binding.animationViewLoading.setAnimation("done.json")
        binding.animationViewLoading.visibility = View.VISIBLE
        binding.animationViewLoading.playAnimation()
        binding.animationViewLoading.addAnimatorListener(object :
            Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator?) {
                binding.textViewTitleOfClues.visibility = View.INVISIBLE
                binding.textViewTitleOfEnigme.visibility = View.INVISIBLE
                binding.recyclerEnigma.visibility = View.INVISIBLE
                binding.recyclerViewClues.visibility = View.INVISIBLE
            }

            override fun onAnimationEnd(p0: Animator?) {

                when (enigmeTag) {
                    "enigmeXXXX" -> findNavController().navigate(R.id.action_gameFragment_to_optionel_enigme_fragment)
                    "enigme1" -> findNavController().navigate(R.id.action_gameFragment_to_enigme1Fragment)
                    "enigme21" -> findNavController().navigate(R.id.action_gameFragment_to_enigme21Fragment)
                    "enigme22" -> findNavController().navigate(R.id.action_gameFragment_to_enigme22Fragment)
                    "enigme3" -> findNavController().navigate(R.id.action_gameFragment_to_enigme3Fragment)
                    "enigme4" -> findNavController().navigate(R.id.action_gameFragment_to_enigme4Fragment)
                }
            }

            override fun onAnimationCancel(p0: Animator?) {

            }

            override fun onAnimationRepeat(p0: Animator?) {

            }
        })
    }

    /******code bluetooth*******/
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action.toString()
            //Log.d("TAG_TEST", action)
            val notificationManager = ContextCompat.getSystemService(
                requireContext(),
                NotificationManager::class.java
            )
            // test code
            when(action) {
                "MESSAGE_FROM_SERVER" -> {
                    val Texte = intent.getStringExtra("MESSAGE_SERVEUR_TO_CLIENT")
                    if(Texte!=""){
                        if(Texte!!.contains("Bien connecté au serveur")){
                            Log.d("Clicke_element","Bien connecté au serveur")
                        }else if(Texte!!.contains("Serveur Bluetooth : serveur fermé .")){
                            Log.d("Clicke_element","serveur fermé")
                        }else if(Texte!!.contains("Serveur Bluetooth fermé : bluetooth desactivé ")){
                            Log.d("Clicke_element","notification normal:"+Texte)
                        }
                        notificationManager?.sendNotificationUpdateDone(requireContext(),"Escape Game",Texte!!)
                    }
                }

            }

        }
    }
    /******end code bluetooth*******/
}