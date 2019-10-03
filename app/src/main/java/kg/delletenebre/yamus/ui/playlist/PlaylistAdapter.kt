package kg.delletenebre.yamus.ui.playlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import kg.delletenebre.yamus.R
import kg.delletenebre.yamus.api.UserModel
import kg.delletenebre.yamus.api.YandexMusic
import kg.delletenebre.yamus.api.response.Track
import kg.delletenebre.yamus.databinding.ListItemPlaylistBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PlaylistAdapter(
        var items: ArrayList<Track>,
        val playlistTrackListener: PlaylistTrackListener?
    ): RecyclerView.Adapter<PlaylistAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemPlaylistBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    interface PlaylistTrackListener {
        fun onClick(track: Track, position: Int)
    }

    inner class ViewHolder(
            private val binding: ListItemPlaylistBinding
        ): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Track) {
            binding.viewHolder = this
            binding.item = item
            binding.executePendingBindings()
        }

        fun onClick(item: Track) {
            playlistTrackListener?.onClick(item, adapterPosition)
        }

        fun onClickMenu(item: Track) {
            val trackId = item.getTrackId()
            val context = binding.root.context
            val popup = PopupMenu(context, binding.settings)
            popup.inflate(R.menu.menu_playlist_item)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.dislike -> {
                        GlobalScope.launch {
                            if (UserModel.getDislikedIds().contains(trackId)) {
                                YandexMusic.removeDislike(trackId)
                            } else {
                                YandexMusic.addDislike(trackId)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }

            if (UserModel.getDislikedIds().contains(trackId)) {
                popup.menu.findItem(R.id.dislike).icon.setTint(context.getColor(R.color.colorAccent))
            } else {
                popup.menu.findItem(R.id.dislike).icon.setTint(context.getColor(R.color.textSecondary))
            }

            val menuHelper = MenuPopupHelper(context, popup.menu as MenuBuilder, binding.settings)
            menuHelper.setForceShowIcon(true)
            menuHelper.show()
        }
    }


}