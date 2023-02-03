package com.example.bluetoothdemo

import android.bluetooth.BluetoothSocket
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ChatFragment : Fragment() {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var chatViewModel: ChatViewModel
    private lateinit var deviceName: TextView
    private lateinit var editedMessage: TextInputEditText
    private lateinit var sendBtn: ImageView
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatMsgAdapter: ChatMsgAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
        deviceName = view.findViewById(R.id.connectedDeviceName)
        editedMessage = view.findViewById(R.id.writeMsg)
        sendBtn = view.findViewById(R.id.sendMsgBtn)
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView)
        chatMsgAdapter = ChatMsgAdapter()

        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatRecyclerView.adapter = chatMsgAdapter

        mainViewModel.connectedServer.observe(viewLifecycleOwner) {
            deviceName.text = it.deviceName
        }

        chatViewModel.latestMsg.observe(viewLifecycleOwner) {
            chatMsgAdapter.submitList(it)
        }

        sendBtn.setOnClickListener {
            val msg = editedMessage.text.toString()
            mainViewModel.writeMsg(msg)
            chatViewModel.setLatestMsg(msg)
            editedMessage.text?.clear()
        }
    }
}