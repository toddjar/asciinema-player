(ns asciinema.player.giffer
  (:require [asciinema.player.view :as view]
            [clojure.set :refer [rename-keys]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as reagent]))

(defn giffer-component [state]
  (let [player-class-name (reaction (view/player-class-name (:theme @state)))
        width (reaction (or (:width @state) 80))
        height (reaction (or (:height @state) 24))
        font-size (reaction (:font-size @state))
        screen (reaction (:screen @state))
        cursor-on (reaction (:cursor-on @state))]
    (fn []
      [:div.asciinema-player {:class-name @player-class-name}
       [view/terminal width height font-size screen cursor-on]])))

(defn create-giffer
  [dom-node frames options]
  (let [dom-node (if (string? dom-node) (.getElementById js/document dom-node) dom-node)
        width 80
        height 24
        font-size "small"
        theme "asciinema"
        [t screen] (first frames)
        player-ratom (reagent/atom {:width width
                                    :height height
                                    :cursor-on true
                                    :font-size font-size
                                    :theme theme
                                    :screen screen})]
    (letfn [(forward [frames]
              (when-let [[t screen] (first frames)]
                (swap! player-ratom assoc :screen screen)
                (reagent/flush)
                (clj->js {:time t
                          :forward #(forward (next frames))})))]
      (reagent/render-component [giffer-component player-ratom] dom-node)
      (reagent/flush)
      (fn []
        (forward (next frames))))))

(defn ^:export CreateGiffer
  [dom-node url options]
  (let [options (-> options
                    (js->clj :keywordize-keys true)
                    (rename-keys {:fontSize :font-size
                                  ;; :onCanPlay :on-can-play
                                  }))
        forward (create-giffer dom-node url options)]
    (clj->js forward)))
