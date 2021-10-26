package com.example.exemple;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.libraries.maps.model.BitmapDescriptor;
import com.google.android.libraries.maps.model.BitmapDescriptorFactory;
import com.google.android.libraries.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UserInfos {

    //User variable
    String pId, pMail, pGivenName, pFamilyName, pName, checkPicture;
    Uri pPicture;

    //Get Sign in info
    public void getInfo(GoogleSignInAccount acct, final MarkerOptions player, Bitmap b) throws InterruptedException {

        //If info are ok
        if (acct != null) {
            String personId = acct.getId();
            String personEmail = acct.getEmail();
            String personGivenName = acct.getGivenName();
            String personFamilyName = acct.getFamilyName();
            String personName = acct.getDisplayName();
            Uri personPicture = acct.getPhotoUrl();

            pId = personId;
            pMail = personEmail;
            pGivenName = personGivenName;
            pFamilyName = personFamilyName;
            pName = personName;
            pPicture = personPicture;
            checkPicture = String.valueOf(pPicture);

            final int height = 150;
            final int width = 150;

            //Picture check for avatar
            if (!checkPicture.equals("null")) {

                //Running in thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        InputStream is = null;
                        try {
                            is = (InputStream) new URL(String.valueOf(pPicture)).getContent();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Bitmap d = BitmapFactory.decodeStream(is);
                        Bitmap b = Bitmap.createScaledBitmap(d, width, height, false);
                        BitmapDescriptor pI = BitmapDescriptorFactory.fromBitmap(b);
                        player.icon(pI);
                    }
                }).start();
                Thread.sleep(5000);
            }else {
                Bitmap smallPlayer = Bitmap.createScaledBitmap(b, width, height, false);
                BitmapDescriptor pI = BitmapDescriptorFactory.fromBitmap(smallPlayer);
                player.icon(pI);
            }
            player.title(pName);
        }
    }
}
