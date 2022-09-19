package com.es.multivs.data.database

import androidx.room.TypeConverter
import com.es.multivs.data.models.AnsweredQuestion
import com.es.multivs.data.models.Question
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * Created by Marko on 10/19/2021.
 * Etrog Systems LTD.
 */
class Converters {

    @TypeConverter
     fun fromDeviceString(value:String): ArrayList<String>{
        val listType: Type = object : TypeToken<ArrayList<String?>?>() {}.type
        return Gson().fromJson(value,listType)
     }

    @TypeConverter
    fun fromDeviceList(list: ArrayList<String>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromQuestionArrayString(value:String): ArrayList<Question> {
        val listType: Type = object : TypeToken<ArrayList<Question?>?>() {}.type
        return Gson().fromJson(value,listType)
    }

    @TypeConverter
    fun fromQuestionArray(list: ArrayList<Question>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromPostQuestionArrayString(value:String): ArrayList<AnsweredQuestion> {
        val listType: Type = object : TypeToken<ArrayList<AnsweredQuestion?>?>() {}.type
        return Gson().fromJson(value,listType)
    }

    @TypeConverter
    fun fromPostQuestionArray(list: ArrayList<AnsweredQuestion>): String {
        return Gson().toJson(list)
    }

//    @TypeConverter
//    fun fromQuestionString(value:String): Question {
//        val questionType: Type = object : TypeToken<Question?>() {}.type
//        return Gson().fromJson(value,questionType)
//    }
//
//    @TypeConverter
//    fun fromQuestion(question: Question): String {
//        return Gson().toJson(question)
//    }

//    @TypeConverter
//    fun fromMedicationString(value:String): ArrayList<MedicationData>{
//        val listType: Type = object : TypeToken<ArrayList<MedicationData?>?>() {}.type
//        return Gson().fromJson(value,listType)
//    }
//
//    @TypeConverter
//    fun fromMedicationList(list: ArrayList<MedicationData>): String {
//        return Gson().toJson(list)
//    }
//
//    @TypeConverter
//    fun fromCalibrationString(value:String): ArrayList<CalibrationData>{
//        val listType: Type = object : TypeToken<ArrayList<CalibrationData?>?>() {}.type
//        return Gson().fromJson(value,listType)
//    }
//
//    @TypeConverter
//    fun fromCalibrationList(list: ArrayList<CalibrationData>): String {
//        return Gson().toJson(list)
//    }
}