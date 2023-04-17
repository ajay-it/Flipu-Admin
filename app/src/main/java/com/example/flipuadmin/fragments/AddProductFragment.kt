package com.example.flipuadmin.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.flipuadmin.R
import com.example.flipuadmin.adapter.AddProductImageAdapter
import com.example.flipuadmin.databinding.FragmentAddProductBinding
import com.example.flipuadmin.model.AddProductModel
import com.example.flipuadmin.model.CategoryModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class AddProductFragment : Fragment() {

    private lateinit var binding: FragmentAddProductBinding
    private lateinit var list: ArrayList<Uri>
    private lateinit var listImages: ArrayList<String>
    private lateinit var adapter: AddProductImageAdapter
    private var coverImage: Uri? = null
    private lateinit var dialog: Dialog
    private var coverImageUrl: String? = ""
    private lateinit var categoryList: ArrayList<String>

    private var launchGalleryActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            coverImage = it.data!!.data
            binding.productCoverImage.setImageURI(coverImage)
            binding.productCoverImage.visibility = VISIBLE
        }
    }

    private var launchProductActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val imageUrl = it.data!!.data
            list.add(imageUrl!!)
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAddProductBinding.inflate(layoutInflater)

        list = ArrayList()
        listImages = ArrayList()
        dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.progress_layout)
        dialog.setCancelable(true)

        binding.selectCoverImg.setOnClickListener {
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type = "image/*"
            launchGalleryActivity.launch(intent)
        }

        binding.productImgBtn.setOnClickListener {
            val intent = Intent("android.intent.action.GET_CONTENT")
            intent.type = "image/*"
            launchProductActivity.launch(intent)
        }

        setProductCategory()

        adapter = AddProductImageAdapter(list)
        binding.productImgRecyclerView.adapter = adapter

        binding.submitBtn.setOnClickListener {
            validateData()
        }

        return binding.root
    }

    private fun setProductCategory() {
        categoryList = ArrayList()
        Firebase.firestore.collection("categories").get().addOnSuccessListener {
            for (doc in it.documents) {
                val data = doc.toObject(CategoryModel::class.java)
                categoryList.add(data!!.cate!!)
            }
            val arrayAdapter =
                ArrayAdapter(requireContext(), R.layout.dropdown_item_layout, categoryList)
            binding.productCategoryDropdown.adapter = arrayAdapter
        }
    }

    private fun validateData() {
        if (binding.etProductName.text.toString().isEmpty()) {
            binding.etProductName.requestFocus()
            binding.etProductName.error = "Empty"
        } else if (binding.etProductDesc.text.toString().isEmpty()) {
            binding.etProductDesc.requestFocus()
            binding.etProductDesc.error = "Empty"
        } else if (binding.etProductMRP.text.toString().isEmpty()) {
            binding.etProductMRP.requestFocus()
            binding.etProductMRP.error = "Empty"
        } else if (binding.etProductSP.text.toString().isEmpty()) {
            binding.etProductSP.requestFocus()
            binding.etProductSP.error = "Empty"
        } else if (coverImage == null) {
            Toast.makeText(requireContext(), "Please select cover image", Toast.LENGTH_SHORT).show()
        } else if (list.size < 1) {
            Toast.makeText(requireContext(), "Please select product images", Toast.LENGTH_SHORT)
                .show()
        } else {
            uploadImage()
        }
    }

    private fun uploadImage() {
        dialog.show()

        val fileName = UUID.randomUUID().toString() + ".jpg"
        val refStorage = FirebaseStorage.getInstance().reference.child("products/$fileName")
        refStorage.putFile(coverImage!!)
            .addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener { image ->
                    coverImageUrl = image.toString()
                    uploadProductImage()
                }
            }
            .addOnFailureListener {
                dialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    "Something went wrong with storage",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private var i = 0
    private fun uploadProductImage() {
        dialog.show()

        val fileName = UUID.randomUUID().toString() + ".jpg"
        val refStorage = FirebaseStorage.getInstance().reference.child("products/$fileName")
        refStorage.putFile(list[i]!!)
            .addOnSuccessListener {
                it.storage.downloadUrl.addOnSuccessListener { image ->
                    listImages.add(image!!.toString())
                    if (list.size == listImages.size) {
                        storeData()
                    } else {
                        i += 1
                        uploadProductImage()
                    }
                }
                    .addOnFailureListener {
                        dialog.dismiss()
                        Toast.makeText(
                            requireContext(),
                            "Something went wrong with storage",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }

    private fun storeData() {
        val db = Firebase.firestore.collection("products")
        val key = db.document().id

        val data = AddProductModel(
            binding.etProductName.text.toString(),
            binding.etProductDesc.text.toString(),
            coverImageUrl.toString(),
            categoryList[binding.productCategoryDropdown.selectedItemPosition],
            key,
            binding.etProductMRP.text.toString(),
            binding.etProductSP.text.toString(),
            listImages
        )

        db.document(key).set(data).addOnSuccessListener {
            dialog.dismiss()
            Toast.makeText(requireContext(), "Product Added", Toast.LENGTH_SHORT).show()
            binding.etProductName.text = null
            binding.etProductDesc.text = null
            binding.etProductSP.text = null
            binding.etProductMRP.text = null
            binding.productCategoryDropdown.setSelection(0);
            binding.productCoverImage.visibility = GONE
            binding.productImgRecyclerView.adapter = null
        }.addOnFailureListener{
            dialog.dismiss()
            Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show()

        }
    }
}


