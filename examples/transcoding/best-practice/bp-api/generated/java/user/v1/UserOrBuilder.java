// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: user/v1/user.proto

// Protobuf Java Version: 3.25.1
package user.v1;

public interface UserOrBuilder extends
    // @@protoc_insertion_point(interface_extends:user.v1.User)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>int64 id = 1 [json_name = "id"];</code>
   * @return The id.
   */
  long getId();

  /**
   * <code>string name = 2 [json_name = "name"];</code>
   * @return The name.
   */
  java.lang.String getName();
  /**
   * <code>string name = 2 [json_name = "name"];</code>
   * @return The bytes for name.
   */
  com.google.protobuf.ByteString
      getNameBytes();

  /**
   * <code>.user.v1.User.Gender gender = 3 [json_name = "gender"];</code>
   * @return The enum numeric value on the wire for gender.
   */
  int getGenderValue();
  /**
   * <code>.user.v1.User.Gender gender = 3 [json_name = "gender"];</code>
   * @return The gender.
   */
  user.v1.User.Gender getGender();
}
