/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.gradleplugin.userinterface.swing.generic;

import org.gradle.gradleplugin.foundation.GradlePluginLord;
import org.gradle.gradleplugin.userinterface.swing.generic.tabs.GradleTab;

import javax.swing.JComponent;

/**
 .

 @author mhunsicker
 */
public interface BasicGradleUI
{
   public GradlePluginLord getGradlePluginLord();

/*
      @return the panel for this pane. This can be inserted directly into your UI.
      @author mhunsicker
   */
   public JComponent getComponent();

   /*
      Call this whenever you're about to show this panel. We'll do whatever
      initialization is necessary.
      @author mhunsicker
   */
   public void aboutToShow();

   //
            public interface CloseInteraction
            {
               /*
                  This is called if gradle tasks are being executed and you want to know if
                  we can close. Ask the user.
                  @return true if the user confirms cancelling the current tasks. False if not.
                  @author mhunsicker
               */
               public boolean promptUserToConfirmClosingWhileBusy();
            }

   /*
      Call this to deteremine if you can close this pane. if we're busy, we'll
      ask the user if they want to close.

      @param  closeInteraction allows us to interact with the user
      @return true if we can close, false if not.
      @author mhunsicker
   */
   public boolean canClose( CloseInteraction closeInteraction );

   /*
      Call this before you close the pane. This gives it an opportunity to do
      cleanup. You probably should call canClose before this. It gives the
      app a chance to cancel if its busy.
      @author mhunsicker
   */
   public void close();

   /*
      @return the total number of tabs.
      @author mhunsicker
   */
   public int getGradleTabCount();

   /*
      @param  index      the index of the tab
      @return the name of the tab at the specified index.
      @author mhunsicker
   */
   public String getGradleTabName( int index );

   /*
      Call this to execute the given gradle command.

      @param  commandLineArguments the command line arguments to pass to gradle.
      @param displayName           the name displayed in the UI for this command
      @author mhunsicker
   */
   public void executeCommand( String commandLineArguments, String displayName );

   /**
    This refreshes the task tree. Useful if you know you've changed something behind
    gradle's back or when first displaying this UI.
    */
   public void refreshTaskTree();

   /**
       Call this to add one of your own tabs to this. You can call this at any
       time.
       @param  index      where to add the tab
       @param  gradleTab  the tab to add
    */
   public void addGradleTab(int index, GradleTab gradleTab);

    /**
       Call this to remove one of your own tabs from this.
       @param  gradleTab  the tab to remove
    */
    public void removeGradleTab(GradleTab gradleTab);

    public OutputUILord getOutputUILord();

   /**
      Determines if commands are currently being executed or not.
      @return true if we're busy, false if not.
   */
   public boolean isBusy();
}