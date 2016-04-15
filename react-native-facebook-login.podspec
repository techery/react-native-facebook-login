Pod::Spec.new do |s|
  s.name             = "react-native-facebook-login"
  s.version          = "0.1.0"
  s.summary          = "iOS FB login support for React Native apps."
  s.requires_arc = true
    s.author       = { 'facebook' => 'fuckthereactnative@fb.com' }
  s.license      = 'Facebook Platform License'
  s.homepage     = 'n/a'
  s.source       = { :git => "https://github.com/techery/react-native-facebook-login.git" }
  s.source_files = 'iOS/*'
  s.platform     = :ios, "7.0"
  s.dependency 'FBSDKCoreKit'
  s.dependency 'React'
end
