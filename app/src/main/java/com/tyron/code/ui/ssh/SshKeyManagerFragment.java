package com.tyron.code.ui.ssh;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.tyron.code.R;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.transition.MaterialFade;
import com.google.android.material.transition.MaterialFadeThrough;
import com.google.android.material.transition.MaterialSharedAxis;
import com.tyron.completion.progress.ProgressManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.tyron.common.util.AndroidUtilities;
import com.google.android.material.textfield.TextInputLayout;
import org.apache.commons.io.FileUtils;
import com.tyron.common.SharedPreferenceKeys;

import com.tyron.code.util.UiUtilsKt;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.os.Bundle;
import java.util.concurrent.Executors;
import android.os.Build;
import android.os.Environment;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import com.tyron.code.ui.ssh.adapter.SshKeyManagerAdapter;
import android.content.DialogInterface;
import android.content.Context;
import java.io.IOException;
import android.text.Editable;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import android.content.ClipboardManager;
import android.widget.Toast;
import java.io.FileOutputStream;
import java.io.IOException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import com.jcraft.jsch.JSchException;
import android.widget.EditText;
import com.tyron.code.ApplicationLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import android.content.SharedPreferences;

public class SshKeyManagerFragment extends Fragment {

    public static final String TAG = SshKeyManagerFragment.class.getSimpleName();
	private RecyclerView mRecyclerView;
    private SshKeyManagerAdapter mAdapter;
	private SharedPreferences mPreferences  = ApplicationLoader.getDefaultPreferences();
	private File sshDir;

	private File privateKey;

	private File publicKey;
		
	public static SshKeyManagerFragment newInstance( ) {
             SshKeyManagerFragment fragment = new SshKeyManagerFragment();
     	
        return fragment;
    }
	
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.ssh_key_manager_fragment, container, false);
        view.setClickable(true);
	
		Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
			getParentFragmentManager().popBackStack());
     

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(menu -> getParentFragmentManager().popBackStackImmediate());

        View fab = view.findViewById(R.id.fab_add_ssh_key);
        UiUtilsKt.addSystemWindowInsetToMargin(fab, false, false, false, true);
        ViewCompat.requestApplyInsets(fab);
		
		mAdapter = new SshKeyManagerAdapter();
		mAdapter.OnSshKeysLongClickedListener(this::inflateSshKeysMenus);
        mRecyclerView = view.findViewById(R.id.ssh_keys_recycler);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.setAdapter(mAdapter);

		loadSshKeys();

		fab.setOnClickListener(v -> {
            LayoutInflater inflater = (LayoutInflater) requireContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			View vie  = inflater.inflate( R.layout.base_textinput_layout, null );
			TextInputLayout layout = vie.findViewById(R.id.textinput_layout);
			layout.setHint("Key name");		
			final Editable keyName = layout.getEditText().getText();
			
			new MaterialAlertDialogBuilder(requireContext())
				.setTitle("Generate Key")
				.setView(vie)
				.setPositiveButton("Generate", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dia, int which) {
						
						generateSshKeys(keyName.toString());
	
					}
					})
	     		.setNegativeButton(android.R.string.cancel, null)
				.show();
			
		});
        
		
	}

	private void generateSshKeys(String keyName) {
			
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			sshDir = new File( requireContext().getExternalFilesDir("/.ssh").getAbsolutePath());
		} else {
			sshDir = new File( Environment.getExternalStorageDirectory()+ "/.ssh");
		}

		if (sshDir.exists()) {

		} else {
			sshDir.mkdirs();
		}
		
		privateKey = new File(sshDir.getAbsolutePath(), keyName+ ".key");
		publicKey = new File(sshDir.getAbsolutePath(), keyName+ ".pub");
		
		if (privateKey.exists()) {
			Toast toast = Toast.makeText(requireContext(), "Private key already exists", Toast.LENGTH_LONG); 
			toast.show();
			return;
			}
		if (keyName.isEmpty()) {
			Toast toast = Toast.makeText(requireContext(), "Key name can't be empty", Toast.LENGTH_LONG); 
			toast.show();
			return;
        } else {
			
			try {
				
           JSch jsch = new JSch();
            KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 4096);
			keyPair.writePrivateKey(new FileOutputStream(privateKey));
			keyPair.writePublicKey(new FileOutputStream(publicKey), "codeassist");
			keyPair.dispose();
									
			} catch (Exception e) {	
				privateKey.delete();
				if (getActivity() != null) {
					requireActivity().runOnUiThread(() ->
					AndroidUtilities.showSimpleAlert(requireContext(),
													 getString(R.string.error),
													 e.getMessage()));
				}
				}
				mPreferences.edit()
				.putString(SharedPreferenceKeys.SSH_KEY_NAME, privateKey.getName())
				.apply();						
				loadSshKeys();
			Toast toast = Toast.makeText(requireContext(), "Key is loading...", Toast.LENGTH_LONG); 
			toast.show();
				}}
				
		
		
	private void loadSshKeys() {
        toggleLoading(true);

        Executors.newSingleThreadExecutor().execute(() -> {
            String path;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                path = requireContext().getExternalFilesDir("/.ssh").getAbsolutePath();
            } else {
                path = Environment.getExternalStorageDirectory()+ "/.ssh";
            }
            File sshKeysDir = new File(path);
			if (sshKeysDir.exists()) {

			} else {
				sshKeysDir.mkdirs();
			}
            File[] files = sshKeysDir.listFiles(File::isFile);

            List<SshKeys> sshKeys = new ArrayList<>();
            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                for (File file : files) {
              
			if (file.exists()) {
                        SshKeys sshkeys = new SshKeys(new File(file.getAbsolutePath()
															   .replaceAll("%20", " ")));
                     if (sshkeys.getRootFile().getName().endsWith(".key")) {
                        sshKeys.add(sshkeys);
                  }  
				if (sshkeys.getRootFile().getName().endsWith(".pub")) {
					sshKeys.add(sshkeys);
				}  
                    }
                }
            }

            if (getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    toggleLoading(false);
                    ProgressManager.getInstance().runLater(() -> {
                        mAdapter.submitList(sshKeys);
                        toggleNullProject(sshKeys);
                    }, 300);
                });
            }
        });
    }

    private void toggleNullProject(List<SshKeys> sshkeys) {
        ProgressManager.getInstance().runLater(() -> {
            if (getActivity() == null || isDetached()) {
                return;
            }
            View view = getView();
            if (view == null) {
                return;
            }

            View recycler = view.findViewById(R.id.ssh_keys_recycler);
            View empty = view.findViewById(R.id.empty_ssh_keys);
			View fab = view.findViewById(R.id.fab_add_ssh_key);
			fab.setVisibility(View.GONE);
            TransitionManager.beginDelayedTransition(
				(ViewGroup) recycler.getParent(), new MaterialFade());
            if (sshkeys.size() == 0) {
                recycler.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
				fab.setVisibility(View.VISIBLE);
            } else {
                recycler.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
            }
        }, 300);
    }

    private void toggleLoading(boolean show) {
        ProgressManager.getInstance().runLater(() -> {
            if (getActivity() == null || isDetached()) {
                return;
            }
            View view = getView();
            if (view == null) {
                return;
            }
            View recycler = view.findViewById(R.id.ssh_keys_recycler);
            View empty = view.findViewById(R.id.empty_container);
            View empty_ssh_keys= view.findViewById(R.id.empty_ssh_keys);
			View fab = view.findViewById(R.id.fab_add_ssh_key);
			fab.setVisibility(View.GONE);
			empty_ssh_keys.setVisibility(View.GONE);

            TransitionManager.beginDelayedTransition((ViewGroup) recycler.getParent(),
                                                     new MaterialFade());
            if (show) {
                recycler.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
            } else {
                recycler.setVisibility(View.VISIBLE);
                empty.setVisibility(View.GONE);
			
            }
        }, 300);
		
    }
	
	private boolean inflateSshKeysMenus(View view, final SshKeys sshkeys) {
		final String privateKeyName = sshkeys.getRootFile().getName();						
		final String privateKeyPath = sshkeys.getRootFile().getAbsolutePath();
		
		String[] option = {"Delete", "Show Content"};
		
		new MaterialAlertDialogBuilder(requireContext())
			.setItems(option, new DialogInterface.OnClickListener() {			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
									case 0:
							String message = getString(R.string.dialog_confirm_delete,
													   sshkeys.getRootFile().getName());
							new MaterialAlertDialogBuilder(requireContext())
								.setTitle(R.string.dialog_delete)
								.setMessage(message)
								.setPositiveButton(android.R.string.yes,
												   (d, w) -> deleteSshKeys(sshkeys))
							.setNegativeButton(android.R.string.no, null)
								.show();
							break;	

						case 1:
							
							
							try {
								
				
								String content = new String(Files.readAllBytes(Paths.get(privateKeyPath)));

								requireActivity().runOnUiThread(() -> {
								
									new MaterialAlertDialogBuilder(requireContext())
										.setTitle(privateKeyName)
										.setMessage(content)
										.setPositiveButton(android.R.string.copy,
														   (d, w) -> copyContent(content))

									.show();	
									
								});
							
								
							} catch (IOException e) {
								if (getActivity() != null) {
									requireActivity().runOnUiThread(() ->
									AndroidUtilities.showSimpleAlert(requireContext(),
																	 getString(R.string.error),
																	 e.getMessage()));
								}
								}
							
							
						break;	
												
					}
				}
			})
			.show();		

		return true;	

    }


    private void deleteSshKeys(SshKeys sshkeys) {

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                FileUtils.forceDelete(sshkeys.getRootFile());
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() -> {
                        Toast toast = Toast.makeText(requireContext(), R.string.delete_success, Toast.LENGTH_LONG); 
						toast.show();
                        loadSshKeys();
                    });
                }
            } catch (IOException e) {
                if (getActivity() != null) {
                    requireActivity().runOnUiThread(() ->
					AndroidUtilities.showSimpleAlert(requireContext(),
													 getString(R.string.error),
													 e.getMessage()));
                }
            }
        });
    }

    	private void copyContent(String content) {
		ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE); 
		clipboard.setText(content);		
		Toast toast = Toast.makeText(requireContext(), R.string.copied_to_clipboard, Toast.LENGTH_LONG); 
		toast.show();
	}
	}