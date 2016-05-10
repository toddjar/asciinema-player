(ns asciinema.player.actions.impl
  (:require [cljs.core.async :refer [chan close!]]
   [asciinema.player.util :as util]
            [asciinema.player.actions :as a]
            [asciinema.player.frames :as f]
            [asciinema.player.source :as source]))

;;; UI originated actions

(defn new-start-at
  "Returns time adjusted by given offset, clipped to the range 0..total-time."
  [current-time total-time offset]
  (util/adjust-to-range (+ current-time offset) 0 total-time))

(defn change-speed [change-fn {:keys [speed source] :as player}]
  (let [new-speed (change-fn speed)]
    (source/change-speed source new-speed)
    (assoc player :speed new-speed)))

(extend-protocol a/Update
  a/FastForward
  (update-player [this {:keys [current-time duration source] :as player}]
    (when duration
      (let [new-time (new-start-at current-time duration 5)]
        (source/seek source new-time)))
    player)

  a/Rewind
  (update-player [this {:keys [current-time duration source] :as player}]
    (when duration
      (let [new-time (new-start-at current-time duration -5)]
        (source/seek source new-time)))
    player)

  a/Seek
  (update-player [this {:keys [duration source] :as player}]
    (when duration
      (let [new-time (* (:position this) duration)]
        (source/seek source new-time)))
    player)

  a/SpeedDown
  (update-player [this player]
    (change-speed #(/ % 2) player))

  a/SpeedUp
  (update-player [this player]
    (change-speed #(* % 2) player))

  a/TogglePlay
  (update-player [this player]
    (source/toggle (:source player))
    player))

;; Internal actions

(extend-protocol a/Update
  a/ShowCursor
  (update-player [this player]
    (assoc player :cursor-on (:show this)))

  a/ShowHud
  (update-player [this player]
    (assoc player :show-hud (:show this))))

;;; Source originated actions

(extend-protocol a/Update
  a/Resize
  (update-player [this {player-width :width player-height :height :as player}]
    (-> player
        (assoc :width (or player-width (:width this)))
        (assoc :height (or player-height (:height this)))))

  a/SetDuration
  (update-player [this player]
    (assoc player :duration (:duration this)))

  a/SetLoading
  (update-player [this player]
    (assoc player :loading (:loading this)))

  a/UpdateTime
  (update-player [this player]
    (assoc player :current-time (:time this))))

(defn blinks
  "Infinite seq of frames with cursor blink action."
  []
  (map #(f/frame %1 (a/->ShowCursor %2))
       (iterate #(+ % 0.5) 0.5)
       (cycle [false true])))

(defn start-blinking [player]
  (let [cursor-blink-ch (source/emit-coll (blinks))]
    (-> player
        (assoc :cursor-on true)
        (assoc :cursor-blink-ch cursor-blink-ch))))

(defn stop-blinking [{:keys [cursor-blink-ch] :as player}]
  (close! cursor-blink-ch)
  (-> player
      (assoc :cursor-on true)
      (assoc :cursor-blink-ch nil)))

(defn restart-blinking [{:keys [cursor-blink-ch] :as player}]
  (if cursor-blink-ch
    (-> player
        stop-blinking
        start-blinking)
    player))

(defn blink-chan-set [{:keys [cursor-blink-ch]}]
  (if cursor-blink-ch
    #{cursor-blink-ch}
    #{}))

(extend-type a/SetPlaying
  a/Update
  (update-player [this player]
    (let [playing (:playing this)
          player (assoc player :playing playing :loaded true)]
      (if playing
        (start-blinking player)
        (stop-blinking player))))

  a/ChannelSource
  (get-channels [this player]
    (blink-chan-set player)))

(extend-type a/UpdateScreen
  a/Update
  (update-player [this player]
    (-> player
        (assoc :screen (:screen this))
        restart-blinking))

  a/ChannelSource
  (get-channels [this player]
    (blink-chan-set player)))
