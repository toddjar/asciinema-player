(ns asciinema.player.actions)

(defprotocol Update
  (update-player [this player]))

(defprotocol ChannelSource
  (get-channels [this player]))

;;; UI originated actions

(defrecord FastForward [])

(defrecord Rewind [])

(defrecord Seek [position])

(defrecord SpeedDown [])

(defrecord SpeedUp [])

(defrecord TogglePlay [])

;; Internal actions

(defrecord ShowCursor [show])

(defrecord ShowHud [show])

;;; Source originated actions

(defrecord Resize [width height])

(defrecord SetDuration [duration])

(defrecord SetLoading [loading])

(defrecord SetPlaying [playing])

(defrecord UpdateScreen [screen])

(defrecord UpdateTime [time])
