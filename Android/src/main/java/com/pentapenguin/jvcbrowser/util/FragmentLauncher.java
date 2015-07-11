package com.pentapenguin.jvcbrowser.util;

import android.support.v4.app.Fragment;

public interface FragmentLauncher {

    void launch(Fragment fragment, boolean isBackStacked);
}
